package com.nextfit.service;

import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Payment;
import com.nextfit.model.Subscription;

/**
 * Result of selling a membership: everything needed to print the receipt.
 *
 * @param subscription the subscription that was created
 * @param payment      the payment that was registered
 * @param plan         the plan that was sold
 */
public record MembershipSale(Subscription subscription, Payment payment, MembershipPlan plan) {
}
