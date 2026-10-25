package com.nextfit.service;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.PersistenceException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private FakePaymentRepository paymentRepository;
    private SubscriptionService subscriptions;
    private PaymentService service;
    private Member member;
    private MembershipPlan monthly;

    @BeforeEach
    void setUp() {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(new FakePlanRepository(),
                new MembershipPlanValidator());
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZONE);
        subscriptions = new SubscriptionService(new FakeSubscriptionRepository(), plans, members, clock);
        paymentRepository = new FakePaymentRepository();
        service = new PaymentService(paymentRepository, subscriptions, plans, new PaymentValidator(), clock);
        member = members.register(FakeRepositories.newMember("1065123456"));
        monthly = plans.create(MembershipPlanServiceTest.newPlan("Mensual", 30, 80_000));
    }

    @Test
    void sellingCreatesTheSubscriptionAndThePayment() {
        MembershipSale sale = service.sellMembership(member.getId(), monthly.getId(), null,
                PaymentMethod.CASH, null, "admin");

        assertEquals(80_000, sale.payment().getTotal());
        assertEquals("NF-000001", sale.payment().getReceiptNumber());
        assertEquals(sale.subscription().getId(), sale.payment().getSubscriptionId());
        assertEquals("admin", sale.payment().getRegisteredBy());
        assertEquals(new NoDiscount().getType(), sale.payment().getDiscount().getType());
        assertEquals(true, subscriptions.hasActiveSubscription(member.getId()));
    }

    @Test
    void aPercentageDiscountLowersTheTotal() {
        MembershipSale sale = service.sellMembership(member.getId(), monthly.getId(), null,
                PaymentMethod.CARD, new PercentageDiscount(10), "admin");

        assertEquals(8_000, sale.payment().getDiscountAmount());
        assertEquals(72_000, sale.payment().getTotal());
    }

    @Test
    void aFixedDiscountNeverMakesTheTotalNegative() {
        MembershipSale sale = service.sellMembership(member.getId(), monthly.getId(), null,
                PaymentMethod.TRANSFER, new FixedDiscount(500_000), "admin");

        assertEquals(0, sale.payment().getTotal());
    }

    @Test
    void theStoredPriceDoesNotChangeWhenThePlanPriceChanges() {
        MembershipSale sale = service.sellMembership(member.getId(), monthly.getId(), null,
                PaymentMethod.CASH, null, "admin");
        long paymentId = sale.payment().getId();

        // The plan is edited afterwards: the old receipt keeps its price.
        assertEquals(80_000, service.findById(paymentId).getSubtotal());
    }

    @Test
    void aMissingMethodIsRejectedBeforeCreatingAnything() {
        assertThrows(InvalidDataException.class, () -> service.sellMembership(
                member.getId(), monthly.getId(), null, null, null, "admin"));
        assertThrows(InvalidDataException.class, () -> service.sellMembership(
                member.getId(), monthly.getId(), null, PaymentMethod.CASH, null, " "));

        assertFalse(subscriptions.hasActiveSubscription(member.getId()));
        assertEquals(0, service.findAll().size());
    }

    @Test
    void ifThePaymentCannotBeStoredTheSubscriptionIsCancelled() {
        paymentRepository.failOnSave(true);

        assertThrows(PersistenceException.class, () -> service.sellMembership(
                member.getId(), monthly.getId(), null, PaymentMethod.CASH, null, "admin"));

        assertFalse(subscriptions.hasActiveSubscription(member.getId()));
        assertEquals(true, subscriptions.findByMember(member.getId()).get(0).isCancelled());
    }

    @Test
    void unknownPaymentIsReported() {
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99));
    }

    @Test
    void recentPaymentsComeNewestFirstAndRespectTheLimit() {
        savePayment(LocalDateTime.of(2026, 9, 10, 9, 0), 50_000);
        savePayment(LocalDateTime.of(2026, 10, 5, 9, 0), 60_000);
        savePayment(LocalDateTime.of(2026, 10, 20, 9, 0), 70_000);

        List<Payment> recent = service.findRecent(2);

        assertEquals(2, recent.size());
        assertEquals(70_000, recent.get(0).getTotal());
        assertEquals(60_000, recent.get(1).getTotal());
    }

    @Test
    void revenueByMonthFillsEmptyMonthsWithZero() {
        savePayment(LocalDateTime.of(2026, 8, 10, 9, 0), 50_000);
        savePayment(LocalDateTime.of(2026, 10, 5, 9, 0), 60_000);
        savePayment(LocalDateTime.of(2026, 10, 20, 9, 0), 70_000);

        Map<YearMonth, Long> result = service.revenueByMonth(3);

        assertEquals(3, result.size());
        assertEquals(50_000L, result.get(YearMonth.of(2026, 8)));
        assertEquals(0L, result.get(YearMonth.of(2026, 9)));
        assertEquals(130_000L, result.get(YearMonth.of(2026, 10)));
    }

    @Test
    void revenueBetweenIncludesBothLimitDays() {
        savePayment(LocalDateTime.of(2026, 10, 1, 8, 0), 50_000);
        savePayment(LocalDateTime.of(2026, 10, 31, 22, 0), 60_000);
        savePayment(LocalDateTime.of(2026, 11, 1, 0, 0), 70_000);

        assertEquals(110_000, service.revenueBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
    }

    @Test
    void revenueByMethodListsEveryMethodEvenWithZero() {
        savePayment(LocalDateTime.of(2026, 10, 5, 9, 0), 60_000, PaymentMethod.CASH);
        savePayment(LocalDateTime.of(2026, 10, 6, 9, 0), 40_000, PaymentMethod.CASH);

        Map<PaymentMethod, Long> result = service.revenueByMethod(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(PaymentMethod.values().length, result.size());
        assertEquals(100_000L, result.get(PaymentMethod.CASH));
        assertEquals(0L, result.get(PaymentMethod.CARD));
    }

    private void savePayment(LocalDateTime when, long price) {
        savePayment(when, price, PaymentMethod.CASH);
    }

    private void savePayment(LocalDateTime when, long price, PaymentMethod method) {
        paymentRepository.save(new Payment(0, 1, member.getId(), when, method, price, new NoDiscount(), "admin"));
    }
}
