package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvPaymentRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PaymentService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the tool "revenue summary" with real CSV files in a temporary folder; today is 13 November 2026. */
class RevenueSummaryToolTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private static final AiUser ADMIN = new AiUser(AiAudience.ADMIN, 0, "admin");

    private CsvPaymentRepository paymentRepository;
    private RevenueSummaryTool tool;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        paymentRepository = new CsvPaymentRepository(folder.resolve("payments.csv"));
        MemberService members = new MemberService(
                new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        SubscriptionService subscriptions = new SubscriptionService(
                new CsvSubscriptionRepository(folder.resolve("subscriptions.csv")), plans, members, CLOCK);
        PaymentService payments = new PaymentService(paymentRepository, subscriptions, plans,
                new PaymentValidator(), CLOCK);
        tool = new RevenueSummaryTool(payments, CLOCK);
    }

    private void pay(LocalDateTime when, PaymentMethod method, long amount) {
        paymentRepository.save(new Payment(0, 1, 1, when, method, amount, new NoDiscount(), "recepcion"));
    }

    private void somePayments() {
        pay(LocalDateTime.of(2026, 11, 13, 9, 0), PaymentMethod.CASH, 80_000);
        pay(LocalDateTime.of(2026, 11, 2, 10, 0), PaymentMethod.CARD, 120_000);
        pay(LocalDateTime.of(2026, 10, 20, 10, 0), PaymentMethod.TRANSFER, 50_000);
    }

    @Test
    void thisMonthIsTheDefaultAndShowsTheMethods() {
        somePayments();

        String text = tool.run(Map.of(), ADMIN);

        assertTrue(text.contains("este mes"));
        assertTrue(text.contains("$200.000"));
        assertTrue(text.contains("Efectivo $80.000"));
        assertTrue(text.contains("Tarjeta $120.000"));
        assertTrue(text.contains("Transferencia $0"));
    }

    @Test
    void todayOnlyCountsToday() {
        somePayments();

        String text = tool.run(Map.of("period", "hoy"), ADMIN);

        assertTrue(text.contains("$80.000"));
        assertFalse(text.contains("$200.000"));
    }

    @Test
    void theMonthBeforeCountsTheWholePreviousMonth() {
        somePayments();

        String text = tool.run(Map.of("period", "mes_anterior"), ADMIN);

        assertTrue(text.contains("el mes anterior"));
        assertTrue(text.contains("1/10/2026 al 31/10/2026"));
        assertTrue(text.contains("$50.000"));
    }

    @Test
    void withoutPaymentsTheTotalIsZero() {
        assertTrue(tool.run(Map.of(), ADMIN).contains("$0"));
    }

    @Test
    void aWrongPeriodIsRejectedWithAClearMessage() {
        assertThrows(InvalidDataException.class, () -> tool.run(Map.of("period", "ayer"), ADMIN));
    }

    @Test
    void onlyTheAdminMayUseIt() {
        assertTrue(tool.allowedFor(AiAudience.ADMIN));
        assertFalse(tool.allowedFor(AiAudience.RECEPTION));
        assertFalse(tool.allowedFor(AiAudience.TRAINER));
        assertFalse(tool.allowedFor(AiAudience.MEMBER));
    }
}