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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private Map<Long, LocalDate> ends;
    private AttendanceService attendance;
    private AccessService service;
    private Member member;

    @BeforeEach
    void setUp() {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        member = members.register(FakeRepositories.newMember("1065123456"));
        ends = new HashMap<>();
        MembershipExpiry expiry = id -> Optional.ofNullable(ends.get(id));
        SubscriptionChecker checker = id -> ends.containsKey(id);
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZONE);
        attendance = new AttendanceService(members, new FakeAttendanceRepository(), checker, clock);
        service = new AccessService(members, attendance, expiry, clock);
    }

    @Test
    void carnetCodeRoundTrips() {
        assertEquals("NF:123", CarnetCode.encode(" 123 "));
        assertEquals(Optional.of("123"), CarnetCode.decode("NF:123"));
        assertEquals(Optional.of("123"), CarnetCode.decode(" nf:123 "));
        assertEquals(Optional.of("123"), CarnetCode.decode("123"));
        assertTrue(CarnetCode.decode("  ").isEmpty());
        assertTrue(CarnetCode.decode(null).isEmpty());
    }

    @Test
    void firstScanEntersAndReportsDaysLeft() {
        ends.put(member.getId(), LocalDate.of(2026, 11, 14));

        AccessResult result = service.process("NF:1065123456");

        assertEquals(Outcome.ENTRY, result.outcome());
        assertTrue(result.granted());
        assertEquals(10, result.daysLeft());
        assertEquals(LocalDate.of(2026, 11, 14), result.validUntil());
        assertEquals(1, attendance.findInside().size());
    }

    @Test
    void secondScanLeaves() {
        ends.put(member.getId(), LocalDate.of(2026, 11, 14));
        service.process("NF:1065123456");

        AccessResult result = service.process("1065123456");

        assertEquals(Outcome.EXIT, result.outcome());
        assertTrue(attendance.findInside().isEmpty());
    }

    @Test
    void memberWithoutMembershipIsDeniedWithAReason() {
        AccessResult result = service.process("NF:1065123456");

        assertEquals(Outcome.DENIED, result.outcome());
        assertFalse(result.granted());
        assertEquals(member.getId(), result.member().getId());
        assertTrue(result.message().contains("membresía"));
        assertTrue(attendance.findInside().isEmpty());
    }

    @Test
    void unknownAndBlankCodesAreDeniedWithoutMember() {
        AccessResult unknown = service.process("NF:999");
        AccessResult blank = service.process("   ");

        assertEquals(Outcome.DENIED, unknown.outcome());
        assertTrue(unknown.member() == null);
        assertEquals(Outcome.DENIED, blank.outcome());
    }

    @Test
    void membershipEndingTodayStillEntersWithZeroDaysLeft() {
        ends.put(member.getId(), LocalDate.of(2026, 11, 4));

        assertEquals(0, service.process("NF:1065123456").daysLeft());
    }
}
