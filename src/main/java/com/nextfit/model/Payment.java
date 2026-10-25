package com.nextfit.model;

import java.time.LocalDateTime;

/**
 * Money received for a subscription. The price and the discount are kept as
 * they were on the day of the payment, so changing a plan's price later never
 * alters past receipts. Amounts are whole Colombian pesos.
 */
public class Payment {

    private static final String RECEIPT_PREFIX = "NF-";

    private final long id;
    private final long subscriptionId;
    private final long memberId;
    private final LocalDateTime paidAt;
    private final PaymentMethod method;
    private final long subtotal;
    private final Discount discount;
    private final String registeredBy;

    /**
     * Creates a payment.
     *
     * @param id             the identifier, or 0 when it has not been assigned yet
     * @param subscriptionId the subscription that was paid
     * @param memberId       the member who paid
     * @param paidAt         when the payment was made
     * @param method         how it was paid
     * @param subtotal       the plan price before the discount
     * @param discount       the discount applied
     * @param registeredBy   the user name of the staff member who registered it
     */
    public Payment(long id, long subscriptionId, long memberId, LocalDateTime paidAt, PaymentMethod method,
                   long subtotal, Discount discount, String registeredBy) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.memberId = memberId;
        this.paidAt = paidAt;
        this.method = method;
        this.subtotal = subtotal;
        this.discount = discount;
        this.registeredBy = registeredBy;
    }

    /**
     * Returns a copy of this payment with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public Payment withId(long newId) {
        return new Payment(newId, subscriptionId, memberId, paidAt, method, subtotal, discount, registeredBy);
    }

    /**
     * Returns how much the discount took off.
     *
     * @return the discounted amount in pesos
     */
    public long getDiscountAmount() {
        return discount.amountOff(subtotal);
    }

    /**
     * Returns what the member actually paid.
     *
     * @return the subtotal minus the discount, in pesos
     */
    public long getTotal() {
        return subtotal - getDiscountAmount();
    }

    /**
     * Returns the receipt number, built from the identifier so it is unique
     * and needs no extra storage.
     *
     * @return the number, e.g. "NF-000042"
     */
    public String getReceiptNumber() {
        return RECEIPT_PREFIX + String.format("%06d", id);
    }

    public long getId() {
        return id;
    }

    public long getSubscriptionId() {
        return subscriptionId;
    }

    public long getMemberId() {
        return memberId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public Discount getDiscount() {
        return discount;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }
}