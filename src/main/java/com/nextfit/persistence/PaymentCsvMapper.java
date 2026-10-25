package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Discount;
import com.nextfit.model.FixedDiscount;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.PercentageDiscount;

import java.time.LocalDateTime;

/**
 * Converts payments to and from CSV rows. The discount is stored as its type
 * and its value.
 */
public final class PaymentCsvMapper {

    /** Header of the payments file. */
    public static final String[] COLUMNS = {"id", "subscriptionId", "memberId", "paidAt", "method",
            "subtotal", "discountType", "discountValue", "registeredBy"};

    private PaymentCsvMapper() {
    }

    /**
     * Converts a payment to a CSV row.
     *
     * @param payment the payment
     * @return the row
     */
    public static String[] toRow(Payment payment) {
        return new String[]{
                String.valueOf(payment.getId()),
                String.valueOf(payment.getSubscriptionId()),
                String.valueOf(payment.getMemberId()),
                payment.getPaidAt().toString(),
                payment.getMethod().name(),
                String.valueOf(payment.getSubtotal()),
                payment.getDiscount().getType(),
                String.valueOf(payment.getDiscount().getValue()),
                payment.getRegisteredBy()
        };
    }

    /**
     * Converts a CSV row to a payment.
     *
     * @param row the row
     * @return the payment
     * @throws PersistenceException when the row is malformed
     */
    public static Payment fromRow(String[] row) {
        try {
            return new Payment(
                    Long.parseLong(row[0]),
                    Long.parseLong(row[1]),
                    Long.parseLong(row[2]),
                    LocalDateTime.parse(row[3]),
                    PaymentMethod.valueOf(row[4]),
                    Long.parseLong(row[5]),
                    discountOf(row[6], Long.parseLong(row[7])),
                    row[8]);
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de pago inválida en el archivo de pagos", e);
        }
    }

    private static Discount discountOf(String type, long value) {
        return switch (type) {
            case NoDiscount.TYPE -> new NoDiscount();
            case PercentageDiscount.TYPE -> new PercentageDiscount((int) value);
            case FixedDiscount.TYPE -> new FixedDiscount(value);
            default -> throw new IllegalArgumentException("Unknown discount type: " + type);
        };
    }
}
