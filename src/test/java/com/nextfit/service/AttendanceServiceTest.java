package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private MemberService memberService;
    private Set<Long> membersWithSubscription;
    private AttendanceService service;
    private Member member;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(new FakeMemberRepository(), new MemberValidator());
        member = memberService.register(FakeRepositories.newMember("1065123456"));
        membersWithSubscription = new HashSet<>();
        SubscriptionChecker fakeChecker = membersWithSubscription::contains;
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZONE);
        service = new AttendanceService(memberService, new FakeAttendanceRepository(), fakeChecker, clock);
    }

    @Test
    void checkInStoresAnOpenRecordWhenTheMemberHasASubscription() {
        membersWithSubscription.add(member.getId());

        AttendanceRecord record = service.checkIn(member.getId());

        assertTrue(record.isOpen());
        assertEquals(1L, service.findInside().size());
    }

    @Test
    void checkInIsRejectedWithoutASubscription() {
        assertThrows(BusinessRuleException.class, () -> service.checkIn(member.getId()));
    }

    @Test
    void checkInIsRejectedWhenTheMemberIsAlreadyInside() {
        membersWithSubscription.add(member.getId());
        service.checkIn(member.getId());

        assertThrows(BusinessRuleException.class, () -> service.checkIn(member.getId()));
    }

    @Test
    void checkInIsRejectedForAnInactiveMember() {
        membersWithSubscription.add(member.getId());
        memberService.deactivate(member.getId());

        assertThrows(BusinessRuleException.class, () -> service.checkIn(member.getId()));
    }

    @Test
    void checkInIsRejectedForAnUnknownMember() {
        assertThrows(ResourceNotFoundException.class, () -> service.checkIn(99));
    }

    @Test
    void checkOutClosesTheOpenRecord() {
        membersWithSubscription.add(member.getId());
        service.checkIn(member.getId());

        AttendanceRecord closed = service.checkOut(member.getId());

        assertFalse(closed.isOpen());
        assertEquals(0L, service.findInside().size());
    }

    @Test
    void checkOutIsRejectedWhenTheMemberIsNotInside() {
        assertThrows(BusinessRuleException.class, () -> service.checkOut(member.getId()));
    }

    @Test
    void countCheckInsOnCountsOnlyThatDay() {
        membersWithSubscription.add(member.getId());
        service.checkIn(member.getId());

        assertEquals(1L, service.countCheckInsOn(LocalDate.of(2026, 10, 27)));
        assertEquals(0L, service.countCheckInsOn(LocalDate.of(2026, 10, 26)));
    }
}