package com.nextfit.service;

/**
 * Answers whether a member currently has a valid membership. Other modules
 * (attendance, training) depend on this small contract instead of on the
 * membership module itself, so they can be tested with a fake one.
 */
public interface SubscriptionChecker {

    /**
     * Tells whether the member has a subscription that is valid today.
     *
     * @param memberId the identifier of the member
     * @return true when the member may use the gym today
     */
    boolean hasActiveSubscription(long memberId);
}