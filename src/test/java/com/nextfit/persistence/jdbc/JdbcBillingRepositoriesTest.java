package com.nextfit.persistence.jdbc;

import com.nextfit.model.Discount;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.model.Subscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plans, subscriptions and payments on an in-memory H2 database. */
class JdbcBillingRepositoriesTest {

    private H2Database db;
    private long memberId;
    private JdbcMembershipPlanRepository plans;
    private JdbcSubscriptionRepository subscriptions;
    private JdbcPaymentRepository payments;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        db = new H2Database();
        memberId = new JdbcMemberRepository(db).save(TestData.member("100")).getId();
        plans = new JdbcMembershipPlanRepository(db);
        subscriptions = new JdbcSubscriptionRepository(db);
        payments = new JdbcPaymentRepository(db);
        MembershipPlan plan = plans.save(TestData.plan("Mensual"));
        subscription = subscriptions.save(new Subscription(0, memberId, plan.getId(),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), false));
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void aPlanIsFoundByNameIgnoringCaseAndKeepsItsPrice() {
        MembershipPlan found = plans.findByName("mensual").orElseThrow();

        assertEquals(50_000L, found.getPrice());
        assertEquals(30, found.getDurationDays());
        assertTrue(plans.findByName("trimestral").isEmpty());
        assertEquals(found.getId(), plans.findById(found.getId()).orElseThrow().getId());
    }

    @Test
    void aPlanCanBeDeactivatedByEditingIt() {
        MembershipPlan plan = plans.findByName("Mensual").orElseThrow();

        plans.save(new MembershipPlan(plan.getId(), plan.getName(), plan.getDescription(),
                plan.getDurationDays(), plan.getPrice(), false));

        assertEquals(1, plans.findAll().size());
        assertTrue(!plans.findById(plan.getId()).orElseThrow().isActive());
    }

    @Test
    void subscriptionsAreFoundByMemberAndCanBeCancelled() {
        assertEquals(1, subscriptions.findByMemberId(memberId).size());

        subscriptions.save(new Subscription(subscription.getId(), memberId, subscription.getPlanId(),
                subscription.getStartDate(), subscription.getEndDate(), true));

        assertTrue(subscriptions.findById(subscription.getId()).orElseThrow().isCancelled());
        assertTrue(subscriptions.findByMemberId(memberId + 99).isEmpty());
    }

    @Test
    void everyKindOfDiscountIsStoredAsTypeAndValueAndRebuiltWithItsOwnClass() {
        Payment percent = payment(new PercentageDiscount(10));
        Payment fixed = payment(new FixedDiscount(5_000L));
        Payment none = payment(new NoDiscount());

        Discount readPercent = payments.findById(percent.getId()).orElseThrow().getDiscount();
        Discount readFixed = payments.findById(fixed.getId()).orElseThrow().getDiscount();
        Discount readNone = payments.findById(none.getId()).orElseThrow().getDiscount();

        assertTrue(readPercent instanceof PercentageDiscount);
        assertEquals(5_000L, readPercent.amountOff(50_000L));
        assertTrue(readFixed instanceof FixedDiscount);
        assertEquals(5_000L, readFixed.amountOff(50_000L));
        assertTrue(readNone instanceof NoDiscount);
        assertEquals(0L, readNone.amountOff(50_000L));
    }

    @Test
    void paymentsAreFoundByMemberAndKeepTheirMomentAndWhoRegisteredThem() {
        Payment saved = payment(new NoDiscount());
        payment(new NoDiscount());

        assertEquals(2, payments.findByMemberId(memberId).size());
        assertEquals(2, payments.findAll().size());
        Payment read = payments.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getPaidAt(), read.getPaidAt());
        assertEquals("recepcion", read.getRegisteredBy());
        assertEquals(50_000L, read.getSubtotal());
        assertTrue(payments.findByMemberId(memberId + 99).isEmpty());
    }

    private Payment payment(Discount discount) {
        return payments.save(new Payment(0, subscription.getId(), memberId,
                LocalDateTime.of(2026, 11, 2, 10, 15, 0), PaymentMethod.values()[0], 50_000L, discount,
                "recepcion"));
    }
}
