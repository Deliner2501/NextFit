package com.nextfit.model;

import com.nextfit.exceptions.InvalidDataException;

/**
 * Takes off a fixed amount of pesos, without going below zero.
 */
public class FixedDiscount extends Discount {

    /** Storage name of this kind of discount. */
    public static final String TYPE = "FIXED";

    private final long amount;

    /**
     * Creates the discount.
     *
     * @param amount the pesos to take off, greater than zero
     * @throws InvalidDataException when the amount is not positive
     */
    public FixedDiscount(long amount) {
        if (amount <= 0) {
            throw new InvalidDataException("descuento", "el valor debe ser mayor que cero");
        }
        this.amount = amount;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public long getValue() {
        return amount;
    }

    @Override
    protected long calculate(long subtotal) {
        return amount;
    }
}