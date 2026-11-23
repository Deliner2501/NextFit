package com.nextfit.model;

import com.nextfit.service.gamification.Badge;

/** A percentage discount earned for a birthday or an attendance achievement. */
public final class RenewalDiscount extends PercentageDiscount {
    public static final String PREFIX = "BENEFIT_";
    public static final String BIRTHDAY = "BIRTHDAY";
    private final String reason;

    /**
     * @param reason the birthday key or a badge identifier
     * @param percent the awarded percentage
     */
    public RenewalDiscount(String reason, int percent) {
        super(percent);
        if (!BIRTHDAY.equals(reason)) {
            Badge.valueOf(reason);
        }
        this.reason = reason;
    }

    /** @return the stable key stored alongside the payment */
    @Override
    public String getType() {
        return PREFIX + reason;
    }

    /** @return the reason in the language used by the interface */
    public String label() {
        return BIRTHDAY.equals(reason) ? "Cumpleaños" : Badge.valueOf(reason).getLabel();
    }
}
