package com.nextfit.ui;

import com.nextfit.model.FixedDiscount;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.model.Subscription;
import com.nextfit.service.MembershipSale;
import com.nextfit.model.Discount;
import com.nextfit.ui.billing.ReceiptText;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptTextTest {

    private static final Member MEMBER = new Member(1, "Ana Pérez", "1065123456", "3001234567",
            "ana@example.com", LocalDate.of(2000, 5, 10), LocalDate.of(2026, 10, 1), "", "Mamá 3110000000", true);
    private static final MembershipPlan PLAN = new MembershipPlan(1, "Mensual", "", 30, 80_000, true);

    private static String receipt(Discount discount) {
        Subscription subscription = new Subscription(1, 1, 1, LocalDate.of(2026, 10, 27), LocalDate.of(2026, 11, 25), false);
        Payment payment = new Payment(1, 1, 1, LocalDateTime.of(2026, 10, 27, 10, 0),
                PaymentMethod.CASH, 80_000, discount, "admin");
        return ReceiptText.of(new MembershipSale(subscription, payment, PLAN), MEMBER);
    }

    @Test
    void receiptShowsNumberMemberPlanPeriodAndTotal() {
        String text = receipt(new NoDiscount());

        assertTrue(text.contains("No. NF-000001"));
        assertTrue(text.contains("Ana Pérez (documento 1065123456)"));
        assertTrue(text.contains("Plan: Mensual (30 días)"));
        assertTrue(text.contains("Vigencia: 27 oct 2026 al 25 nov 2026"));
        assertTrue(text.contains("Método de pago: Efectivo"));
        assertTrue(text.contains("Total pagado: $ 80.000"));
        assertTrue(text.contains("Atendido por: admin"));
    }

    @Test
    void receiptWithoutDiscountHasNoDiscountLine() {
        assertFalse(receipt(new NoDiscount()).contains("Descuento"));
    }

    @Test
    void receiptShowsThePercentageAndTheDiscountedTotal() {
        String text = receipt(new PercentageDiscount(10));

        assertTrue(text.contains("Descuento (10%): -$ 8.000"));
        assertTrue(text.contains("Total pagado: $ 72.000"));
    }

    @Test
    void receiptShowsAFixedDiscountWithoutPercentage() {
        String text = receipt(new FixedDiscount(5_000));

        assertTrue(text.contains("Descuento: -$ 5.000"));
        assertTrue(text.contains("Total pagado: $ 75.000"));
    }
}
