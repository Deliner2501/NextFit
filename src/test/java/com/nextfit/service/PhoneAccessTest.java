package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult.Outcome;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneAccessTest {

    /** A clock the test can move forward. */
    private static final class MovingClock extends Clock {
        private Instant now = Instant.parse("2026-11-05T15:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MovingClock clock;
    private Map<Long, LocalDate> ends;
    private AttendanceService attendance;
    private AccessService access;
    private PhoneAccess phone;
    private Member member;
    private String token;

    @BeforeEach
    void setUp() {
        clock = new MovingClock();
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        member = members.register(FakeRepositories.newMember("1065123456"));
        ends = new HashMap<>();
        ends.put(member.getId(), LocalDate.of(2026, 11, 30));
        MembershipExpiry expiry = id -> Optional.ofNullable(ends.get(id));
        SubscriptionChecker checker = id -> ends.containsKey(id);
        attendance = new AttendanceService(members, new FakeAttendanceRepository(), checker, clock);
        access = new AccessService(members, attendance, expiry, clock);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 3);
        phone = new PhoneAccess(members, access, new AccessTokens(key), clock);
        token = phone.tokenOf(member);
    }

    @Test
    void validTokenRegistersTheEntry() {
        AccessResult result = phone.scan(token);

        assertEquals(Outcome.ENTRY, result.outcome());
        assertEquals(member.getId(), result.member().getId());
        assertEquals(1, attendance.findInside().size());
    }

    @Test
    void aRepeatedScanInAFewSecondsReturnsTheSameAnswerWithoutToggling() {
        phone.scan(token);
        clock.advanceSeconds(3);

        AccessResult again = phone.scan(token);

        assertEquals(Outcome.ENTRY, again.outcome());
        assertEquals(1, attendance.findInside().size());
    }

    @Test
    void aLaterScanRegistersTheExit() {
        phone.scan(token);
        clock.advanceSeconds(PhoneAccess.REPEAT_SECONDS + 1);

        AccessResult exit = phone.scan(token);

        assertEquals(Outcome.EXIT, exit.outcome());
        assertTrue(attendance.findInside().isEmpty());
    }

    @Test
    void unknownOrMalformedCodesAreDenied() {
        assertEquals(Outcome.DENIED, phone.scan("ABCDEFGHJK").outcome());
        assertEquals(Outcome.DENIED, phone.scan("corto").outcome());
        assertEquals(Outcome.DENIED, phone.scan(null).outcome());
        assertTrue(attendance.findInside().isEmpty());
    }

    @Test
    void memberWithoutMembershipIsDeniedAndAnotherTryIsNotCached() {
        ends.clear();

        assertEquals(Outcome.DENIED, phone.scan(token).outcome());
        ends.put(member.getId(), LocalDate.of(2026, 11, 30));
        assertEquals(Outcome.ENTRY, phone.scan(token).outcome());
    }

    @Test
    void listenersReceiveEveryResultIncludingPhoneScans() {
        List<AccessResult> seen = new ArrayList<>();
        access.addListener(seen::add);

        phone.scan(token);
        phone.scan("ZZZZZZZZZZ");
        access.process("NF:1065123456");

        assertEquals(3, seen.size());
        assertEquals(Outcome.ENTRY, seen.get(0).outcome());
        assertEquals(Outcome.DENIED, seen.get(1).outcome());
        assertEquals(Outcome.EXIT, seen.get(2).outcome());
        assertFalse(seen.get(1).granted());
    }
}
