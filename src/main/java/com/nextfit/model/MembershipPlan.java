package com.nextfit.model;

import java.time.LocalDate;

/**
 * A membership the gym sells: how long it lasts and how much it costs.
 * Prices are whole Colombian pesos.
 */
public class MembershipPlan {

    private final long id;
    private final String name;
    private final String description;
    private final int durationDays;
    private final long price;
    private final boolean active;

    /**
     * Creates a plan.
     *
     * @param id           the identifier, or 0 when it has not been assigned yet
     * @param name         the name shown to the staff, e.g. "Mensual"
     * @param description  a short explanation of what the plan includes
     * @param durationDays how many days a subscription to this plan lasts
     * @param price        the price in pesos
     * @param active       whether the plan can still be sold
     */
    public MembershipPlan(long id, String name, String description, int durationDays, long price,
                          boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationDays = durationDays;
        this.price = price;
        this.active = active;
    }

    /**
     * Returns a copy of this plan with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public MembershipPlan withId(long newId) {
        return new MembershipPlan(newId, name, description, durationDays, price, active);
    }

    /**
     * Returns a copy of this plan that can or cannot be sold.
     *
     * @param newActive true to allow sales
     * @return the copy
     */
    public MembershipPlan withActive(boolean newActive) {
        return new MembershipPlan(id, name, description, durationDays, price, newActive);
    }

    /**
     * Returns the last day covered by a subscription to this plan that starts
     * on the given day. The starting day counts as the first day.
     *
     * @param start the first day of the subscription
     * @return the last day of the subscription
     */
    public LocalDate endDateFor(LocalDate start) {
        return start.plusDays(durationDays - 1L);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public long getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}