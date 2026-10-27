package com.nextfit.model;

/**
 * The absence of a discount: the member pays the full price.
 */
public class NoDiscount extends Discount {

    /** Storage name of this kind of discount. */
    public static final String TYPE = "NONE";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public long getValue() {
        return 0;
    }

    @Override
    protected long calculate(long subtotal) {
        return 0;
    }
}