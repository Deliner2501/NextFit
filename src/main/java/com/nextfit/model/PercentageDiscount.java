package com.nextfit.model;

import com.nextfit.exceptions.InvalidDataException;

/**
 * Takes off a percentage of the price, rounded to the nearest peso.
 */
public class PercentageDiscount extends Discount {

    /** Storage name of this kind of discount. */
    public static final String TYPE = "PERCENT";

    private final int percent;

    /**
     * Creates the discount.
     *
     * @param percent the percentage, from 1 to 100
     * @throws InvalidDataException when the percentage is out of range
     */
    public PercentageDiscount(int percent) {
        if (percent < 1 || percent > 100) {
            throw new InvalidDataException("descuento", "el porcentaje debe estar entre 1 y 100");
        }
        this.percent = percent;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public long getValue() {
        return percent;
    }

    @Override
    protected long calculate(long subtotal) {
        return Math.round(subtotal * percent / 100.0);
    }
}