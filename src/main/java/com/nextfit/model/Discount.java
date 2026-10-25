package com.nextfit.model;

/**
 * A reduction applied to the price of a membership. Each kind of discount
 * decides how much it takes off; callers only ask for the amount, so a new
 * kind can be added without changing the code that uses discounts.
 * Amounts are whole Colombian pesos.
 */
public abstract class Discount {

    /**
     * Returns the name that identifies this kind of discount in storage.
     *
     * @return the type, e.g. "PERCENT"
     */
    public abstract String getType();

    /**
     * Returns the number that parametrizes the discount (a percentage or an amount).
     *
     * @return the value
     */
    public abstract long getValue();

    /**
     * Calculates how much this kind of discount would take off.
     *
     * @param subtotal the price before the discount
     * @return the raw amount, which {@link #amountOff(long)} limits to a valid range
     */
    protected abstract long calculate(long subtotal);

    /**
     * Returns how much is taken off a price. It is never negative and never
     * more than the price itself.
     *
     * @param subtotal the price before the discount
     * @return the amount to subtract
     */
    public final long amountOff(long subtotal) {
        return Math.max(0, Math.min(subtotal, calculate(subtotal)));
    }
}