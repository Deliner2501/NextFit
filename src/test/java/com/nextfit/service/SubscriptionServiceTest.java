package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Subscription;
import com.nextfit.model.SubscriptionStatus;
import com.nextfit.service.BillingFakeRepositories.FakePlanRepository;
import com.nextfit.service.BillingFakeRepositories.FakeSubscriptionRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private MemberService members;
    private MembershipPlanService plans;
    private SubscriptionService service;
    private Member member;
    private MembershipPlan monthly;

    @BeforeEach
    void setUp() {
        members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        plans = new MembershipPlanService(new FakePlanRepository(), new MembershipPlanValidator());
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZONE);
        service = new SubscriptionService(new FakeSubscriptionRepository(), plans, members, clock);
        member = members.register(FakeRepositories.newMember("1065123456"));
        monthly = plans.create(MembershipPlanServiceTest.newPlan("Mensual", 30, 80_000));
    }

    @Test
    void subscribeStartsTodayWhenNoDateIsGiven() {
        Subscription s = service.subscribe(member.getId(), monthly.getId(), null);

        assertEquals(TODAY, s.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 25), s.getEndDate());
        assertTrue(service.hasActiveSubscription(member.getId()));
    }

    @Test
    void renewalStartsTheDayAfterTheCurrentOneEnds() {
        Subscription first = service.subscribe(member.getId(), monthly.getId(), null);

        Subscription second = service.subscribe(member.getId(), monthly.getId(), null);

        assertEquals(first.getEndDate().plusDays(1), second.getStartDate());
        assertEquals(2, service.findByMember(member.getId()).size());
    }

    @Test
    void aFutureStartDateIsRespectedAndIsNotActiveYet() {
        LocalDate nextWeek = TODAY.plusDays(7);

        Subscription s = service.subscribe(member.getId(), monthly.getId(), nextWeek);

        assertEquals(nextWeek, s.getStartDate());
        assertEquals(SubscriptionStatus.UPCOMING, s.statusOn(TODAY));
        assertFalse(service.hasActiveSubscription(member.getId()));
        assertTrue(service.hasActiveSubscription(member.getId(), nextWeek));
    }

    @Test
    void aStartDateInThePastIsRejected() {
        assertThrows(InvalidDataException.class,
                () -> service.subscribe(member.getId(), monthly.getId(), TODAY.minusDays(1)));
    }

    @Test
    void anInactiveMemberCannotSubscribe() {
        members.deactivate(member.getId());

        assertThrows(BusinessRuleException.class,
                () -> service.subscribe(member.getId(), monthly.getId(), null));
    }

    @Test
    void aPlanNoLongerSoldCannotBeBought() {
        plans.deactivate(monthly.getId());

        assertThrows(BusinessRuleException.class,
                () -> service.subscribe(member.getId(), monthly.getId(), null));
    }

    @Test
    void unknownMemberOrPlanIsReported() {
        assertThrows(ResourceNotFoundException.class, () -> service.subscribe(99, monthly.getId(), null));
        assertThrows(ResourceNotFoundException.class, () -> service.subscribe(member.getId(), 99, null));
    }

    @Test
    void aMemberWithoutSubscriptionHasNoAccess() {
        assertFalse(service.hasActiveSubscription(member.getId()));
        assertTrue(service.findCurrentFor(member.getId()).isEmpty());
    }

    @Test
    void cancelRemovesTheAccessAndCannotBeRepeated() {
        Subscription s = service.subscribe(member.getId(), monthly.getId(), null);

        service.cancel(s.getId());

        assertFalse(service.hasActiveSubscription(member.getId()));
        assertThrows(BusinessRuleException.class, () -> service.cancel(s.getId()));
    }

    @Test
    void expiringListHasOnlyActiveSubscriptionsEndingSoon() {
        MembershipPlan week = plans.create(MembershipPlanServiceTest.newPlan("Semanal", 7, 25_000));
        Member other = members.register(FakeRepositories.newMember("1065999999"));
        service.subscribe(member.getId(), week.getId(), null);
        service.subscribe(other.getId(), monthly.getId(), null);

        var expiring = service.findExpiringWithin(10);

        assertEquals(1, expiring.size());
        assertEquals(member.getId(), expiring.get(0).getMemberId());
        assertEquals(2, service.countActive());
    }
}
