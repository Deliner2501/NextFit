package com.nextfit.persistence;

import com.nextfit.model.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Storage of payments. Services depend on this interface, so the CSV
 * implementation can be replaced by a database one without changing them.
 */
public interface PaymentRepository {

    /**
     * Returns every stored payment.
     *
     * @return the payments
     */
    List<Payment> findAll();

    /**
     * Looks a payment up by identifier.
     *
     * @param id the identifier
     * @return the payment, or empty when it does not exist
     */
    Optional<Payment> findById(long id);

    /**
     * Returns the payments of one member.
     *
     * @param memberId the identifier of the member
     * @return the payments, possibly empty
     */
    List<Payment> findByMemberId(long memberId);

    /**
     * Inserts the payment when its id is 0 (assigning a new one) or replaces
     * the stored payment with the same id.
     *
     * @param payment the payment to store
     * @return the stored payment, with its identifier
     */
    Payment save(Payment payment);
}
