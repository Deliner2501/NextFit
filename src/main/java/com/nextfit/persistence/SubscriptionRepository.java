package com.nextfit.persistence;

import com.nextfit.model.Subscription;

import java.util.List;
import java.util.Optional;

/**
 * Storage of subscriptions. Services depend on this interface, so the CSV
 * implementation can be replaced by a database one without changing them.
 */
public interface SubscriptionRepository {

    /**
     * Returns every stored subscription.
     *
     * @return the subscriptions
     */
    List<Subscription> findAll();

    /**
     * Looks a subscription up by identifier.
     *
     * @param id the identifier
     * @return the subscription, or empty when it does not exist
     */
    Optional<Subscription> findById(long id);

    /**
     * Returns the subscriptions of one member.
     *
     * @param memberId the identifier of the member
     * @return the subscriptions, possibly empty
     */
    List<Subscription> findByMemberId(long memberId);

    /**
     * Inserts the subscription when its id is 0 (assigning a new one) or
     * replaces the stored subscription with the same id.
     *
     * @param subscription the subscription to store
     * @return the stored subscription, with its identifier
     */
    Subscription save(Subscription subscription);
}
