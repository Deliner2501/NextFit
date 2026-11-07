package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.service.BillingFakeRepositories.FakePaymentRepository;
import com.nextfit.service.BillingFakeRepositories.FakePlanRepository;
import com.nextfit.service.BillingFakeRepositories.FakeSubscriptionRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
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

class CashCloseServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private PaymentService payments;
    private CashCloseService service;
    private Member member;
    private MembershipPlan monthly;

    @BeforeEach
    void setUp() {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(new FakePlanRepository(), new MembershipPlanValidator());
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZONE);
        SubscriptionService subscriptions = new SubscriptionService(new FakeSubscriptionRepository(), plans, members, clock);
        payments = new PaymentService(new FakePaymentRepository(), subscriptions, plans, new PaymentValidator(), clock);
        service = new CashCloseService(payments, members);
        member = members.register(FakeRepositories.newMember("1065123456"));
        monthly = plans.create(MembershipPlanServiceTest.newPlan("Mensual", 30, 80_000));
    }

    private void sell(PaymentMethod method, int discountPercent) {
        payments.sellMembership(member.getId(), monthly.getId(), null, method,
                discountPercent == 0 ? null : new PercentageDiscount(discountPercent), "admin");
    }

    @Test
    void anEmptyDayHasZeroEverywhere() {
        CashClose close = service.closeOf(TODAY.minusDays(3));

        assertEquals(0, close.count());
        assertEquals(0, close.total());
        for (PaymentMethod method : PaymentMethod.values()) {
            assertEquals(0L, close.byMethod().get(method).longValue());
        }
        assertFalse(close.isCounted());
        assertEquals(0, close.difference());
    }

    @Test
    void totalsAreSplitByPaymentMethodAndDiscountsAreSummed() {
        sell(PaymentMethod.CASH, 0);
        sell(PaymentMethod.CARD, 10);
        sell(PaymentMethod.CASH, 0);

        CashClose close = service.closeOf(TODAY);

        assertEquals(3, close.count());
        assertEquals(160_000, close.byMethod().get(PaymentMethod.CASH).longValue());
        assertEquals(72_000, close.byMethod().get(PaymentMethod.CARD).longValue());
        assertEquals(0L, close.byMethod().get(PaymentMethod.TRANSFER).longValue());
        assertEquals(232_000, close.total());
        assertEquals(8_000, close.discounts());
        assertEquals(160_000, close.expectedCash());
    }

    @Test
    void linesCarryTheMemberAndThePlanNames() {
        sell(PaymentMethod.CASH, 0);

        CashClose.Line line = service.closeOf(TODAY).lines().get(0);

        assertEquals(member.getFullName(), line.member());
        assertEquals("Mensual", line.plan());
        assertEquals("admin", line.cashier());
        assertEquals(80_000, line.total());
    }

    @Test
    void otherDaysAreNotIncluded() {
        sell(PaymentMethod.CASH, 0);

        assertEquals(0, service.closeOf(TODAY.plusDays(1)).count());
        assertEquals(1, service.linesBetween(TODAY.minusDays(2), TODAY.plusDays(2)).size());
    }

    @Test
    void countedCashGivesTheDifference() {
        sell(PaymentMethod.CASH, 0);
        CashClose close = service.closeOf(TODAY);

        assertEquals(-5_000, close.withCountedCash(75_000).difference());
        assertEquals(0, close.withCountedCash(80_000).difference());
        assertEquals(2_000, close.withCountedCash(82_000).difference());
        assertTrue(close.withCountedCash(0).isCounted());
        assertEquals(-80_000, close.withCountedCash(0).difference());
    }

    @Test
    void aNegativeCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.closeOf(TODAY).withCountedCash(-1));
    }
}
