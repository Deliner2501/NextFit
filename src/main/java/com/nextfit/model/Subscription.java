package com.nextfit.model;

import com.nextfit.exceptions.InvalidDataException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The right of a member to use the gym between two dates, bought with a
 * membership plan. Both dates are included. Whether it is upcoming, active or
 * expired is not stored: it is worked out from the dates, so it can never get
 * out of date.
 */
public class Subscription {

    private final long id;
    private final long memberId;
    private final long planId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean cancelled;

    /**
     * Creates a subscription.
     *
     * @param id        the identifier, or 0 when it has not been assigned yet
     * @param memberId  the member who owns it
     * @param planId    the plan it was bought with
     * @param startDate the first day it is valid
     * @param endDate   the last day it is valid
     * @param cancelled whether it was cancelled before its end
     * @throws InvalidDataException when the end is before the start
     */
    public Subscription(long id, long memberId, long planId, LocalDate startDate, LocalDate endDate,
                        boolean cancelled) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidDataException("fecha de fin", "no puede ser anterior a la fecha de inicio");
        }
        this.id = id;
        this.memberId = memberId;
        this.planId = planId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cancelled = cancelled;
    }

    /**
     * Returns a copy of this subscription with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public Subscription withId(long newId) {
        return new Subscription(newId, memberId, planId, startDate, endDate, cancelled);
    }

    /**
     * Returns a copy of this subscription marked as cancelled.
     *
     * @return the cancelled copy
     */
    public Subscription cancel() {
        return new Subscription(id, memberId, planId, startDate, endDate, true);
    }

    /**
     * Works out the situation of this subscription on a day.
     *
     * @param day the day to check
     * @return cancelled, upcoming, expired or active
     */
    public SubscriptionStatus statusOn(LocalDate day) {
        if (cancelled) {
            return SubscriptionStatus.CANCELLED;
        }
        if (day.isBefore(startDate)) {
            return SubscriptionStatus.UPCOMING;
        }
        if (day.isAfter(endDate)) {
            return SubscriptionStatus.EXPIRED;
        }
        return SubscriptionStatus.ACTIVE;
    }

    /**
     * Tells whether the member can use the gym on a day.
     *
     * @param day the day to check
     * @return true when the subscription is active that day
     */
    public boolean isActiveOn(LocalDate day) {
        return statusOn(day) == SubscriptionStatus.ACTIVE;
    }

    /**
     * Returns how many days are left, counting from the given day. It is zero
     * when the subscription is not active that day.
     *
     * @param day the day to count from
     * @return the remaining days, never negative
     */
    public long daysLeftOn(LocalDate day) {
        if (!isActiveOn(day)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(day, endDate);
    }

    public long getId() {
        return id;
    }

    public long getMemberId() {
        return memberId;
    }

    public long getPlanId() {
        return planId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}