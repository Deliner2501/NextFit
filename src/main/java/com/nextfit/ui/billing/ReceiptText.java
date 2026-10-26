package com.nextfit.ui.billing;

import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Payment;
import com.nextfit.model.PercentageDiscount;
import com.nextfit.model.Subscription;
import com.nextfit.service.MembershipSale;
import com.nextfit.ui.Formats;

/**
 * Builds the text of a payment receipt. It has no Swing code, so the same text
 * is shown in the receipt window, copied to the clipboard and checked in tests.
 */
public final class ReceiptText {

    private ReceiptText() {
    }

    /**
     * Builds the receipt of a sale.
     *
     * @param sale   the subscription, payment and plan that were sold
     * @param member the member who paid
     * @return the receipt, one line per piece of information
     */
    public static String of(MembershipSale sale, Member member) {
        Payment payment = sale.payment();
        Subscription subscription = sale.subscription();
        MembershipPlan plan = sale.plan();
        StringBuilder text = new StringBuilder();
        text.append("NEXTFIT - Comprobante de pago\n");
        text.append("No. ").append(payment.getReceiptNumber()).append('\n');
        text.append("Fecha: ").append(Formats.dateTime(payment.getPaidAt())).append('\n');
        text.append("Socio: ").append(member.getFullName())
                .append(" (documento ").append(member.getDocumentNumber()).append(")\n");
        text.append("Plan: ").append(plan.getName())
                .append(" (").append(plan.getDurationDays()).append(" días)\n");
        text.append("Vigencia: ").append(Formats.date(subscription.getStartDate()))
                .append(" al ").append(Formats.date(subscription.getEndDate())).append('\n');
        text.append("Método de pago: ").append(payment.getMethod().getLabel()).append('\n');
        text.append("Subtotal: ").append(Formats.money(payment.getSubtotal())).append('\n');
        if (payment.getDiscountAmount() > 0) {
            text.append(discountLabel(payment)).append(": -")
                    .append(Formats.money(payment.getDiscountAmount())).append('\n');
        }
        text.append("Total pagado: ").append(Formats.money(payment.getTotal())).append('\n');
        text.append("Atendido por: ").append(payment.getRegisteredBy()).append('\n');
        return text.toString();
    }

    private static String discountLabel(Payment payment) {
        if (PercentageDiscount.TYPE.equals(payment.getDiscount().getType())) {
            return "Descuento (" + payment.getDiscount().getValue() + "%)";
        }
        return "Descuento";
    }
}
