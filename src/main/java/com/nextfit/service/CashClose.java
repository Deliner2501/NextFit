package com.nextfit.service;

import com.nextfit.model.PaymentMethod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The money of one day: every payment, the total by payment method and, when the
 * cashier counted the drawer, the difference against the cash the system expects.
 *
 * @param day         the day that is closed
 * @param lines       the payments of the day, oldest first
 * @param byMethod    the total collected with each payment method
 * @param total       the total collected in the day
 * @param discounts   the pesos given away in discounts
 * @param countedCash the cash counted in the drawer, or -1 when it was not counted
 */
public record CashClose(LocalDate day, List<Line> lines, Map<PaymentMethod, Long> byMethod, long total,
                        long discounts, long countedCash) {

    /**
     * One payment of the day.
     *
     * @param receipt  the receipt number
     * @param paidAt   the day and minute of the payment
     * @param member   the name of the member who paid
     * @param plan     the name of the plan that was sold
     * @param method   how it was paid
     * @param subtotal the price before the discount
     * @param discount the pesos taken off
     * @param total    what was charged
     * @param cashier  the user that registered the payment
     */
    public record Line(String receipt, LocalDateTime paidAt, String member, String plan, PaymentMethod method,
                       long subtotal, long discount, long total, String cashier) {
    }

    /**
     * Returns how many payments the day has.
     *
     * @return the number of payments
     */
    public int count() {
        return lines.size();
    }

    /**
     * Returns the cash the system expects to find in the drawer.
     *
     * @return the pesos collected in cash
     */
    public long expectedCash() {
        return byMethod.getOrDefault(PaymentMethod.CASH, 0L);
    }

    /**
     * Tells whether the cashier already counted the drawer.
     *
     * @return true when a count was given
     */
    public boolean isCounted() {
        return countedCash >= 0;
    }

    /**
     * Returns counted cash minus expected cash: positive means extra money, negative means missing money.
     *
     * @return the difference, 0 when the drawer was not counted
     */
    public long difference() {
        return isCounted() ? countedCash - expectedCash() : 0;
    }

    /**
     * Returns a copy of this close with the cash counted by the cashier.
     *
     * @param counted the pesos counted in the drawer, 0 or more
     * @return the copy
     */
    public CashClose withCountedCash(long counted) {
        if (counted < 0) {
            throw new IllegalArgumentException("El efectivo contado no puede ser negativo");
        }
        return new CashClose(day, lines, byMethod, total, discounts, counted);
    }
}
