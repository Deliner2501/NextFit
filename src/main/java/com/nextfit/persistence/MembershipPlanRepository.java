package com.nextfit.persistence;

import com.nextfit.model.MembershipPlan;

import java.util.List;
import java.util.Optional;

/**
 * Storage of membership plans. Services depend on this interface, so the CSV
 * implementation can be replaced by a database one without changing them.
 */
public interface MembershipPlanRepository {

    /**
     * Returns every stored plan.
     *
     * @return the plans
     */
    List<MembershipPlan> findAll();

    /**
     * Looks a plan up by identifier.
     *
     * @param id the identifier
     * @return the plan, or empty when it does not exist
     */
    Optional<MembershipPlan> findById(long id);

    /**
     * Looks a plan up by name, ignoring case.
     *
     * @param name the name
     * @return the plan, or empty when it does not exist
     */
    Optional<MembershipPlan> findByName(String name);

    /**
     * Inserts the plan when its id is 0 (assigning a new one) or replaces the
     * stored plan with the same id.
     *
     * @param plan the plan to store
     * @return the stored plan, with its identifier
     */
    MembershipPlan save(MembershipPlan plan);
}
