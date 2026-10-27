package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.MembershipPlan;
import com.nextfit.persistence.MembershipPlanRepository;
import com.nextfit.validation.MembershipPlanValidator;

import java.util.List;

/**
 * Rules of the membership plans the gym sells. It knows nothing about screens
 * or files: it only uses a {@link MembershipPlanRepository} and a
 * {@link MembershipPlanValidator}.
 */
public class MembershipPlanService {

    private final MembershipPlanRepository plans;
    private final MembershipPlanValidator validator;

    /**
     * Creates the service.
     *
     * @param plans     the plan storage
     * @param validator the checker of plan data
     */
    public MembershipPlanService(MembershipPlanRepository plans, MembershipPlanValidator validator) {
        this.plans = plans;
        this.validator = validator;
    }

    /**
     * Returns every plan, including the ones no longer sold.
     *
     * @return the plans
     */
    public List<MembershipPlan> findAll() {
        return plans.findAll();
    }

    /**
     * Returns only the plans that can be sold.
     *
     * @return the active plans
     */
    public List<MembershipPlan> findActive() {
        return plans.findAll().stream().filter(MembershipPlan::isActive).toList();
    }

    /**
     * Looks a plan up by identifier.
     *
     * @param id the identifier
     * @return the plan
     * @throws ResourceNotFoundException when no plan has that identifier
     */
    public MembershipPlan findById(long id) {
        return plans.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("plan", String.valueOf(id)));
    }

    /**
     * Creates a new plan.
     *
     * @param plan the plan, with identifier 0
     * @return the stored plan
     * @throws com.nextfit.exceptions.InvalidDataException when its data is not valid
     * @throws BusinessRuleException                       when another plan has the same name
     */
    public MembershipPlan create(MembershipPlan plan) {
        validator.validate(plan);
        ensureNameIsFree(plan.getName(), 0);
        return plans.save(plan.withId(0));
    }

    /**
     * Changes an existing plan. Subscriptions already sold keep their dates.
     *
     * @param plan the plan with its new data
     * @return the stored plan
     * @throws ResourceNotFoundException                   when the plan does not exist
     * @throws com.nextfit.exceptions.InvalidDataException when its data is not valid
     * @throws BusinessRuleException                       when another plan has the same name
     */
    public MembershipPlan update(MembershipPlan plan) {
        findById(plan.getId());
        validator.validate(plan);
        ensureNameIsFree(plan.getName(), plan.getId());
        return plans.save(plan);
    }

    /**
     * Stops selling a plan. It is kept so old subscriptions can still show it.
     *
     * @param id the identifier of the plan
     * @return the updated plan
     * @throws ResourceNotFoundException when the plan does not exist
     */
    public MembershipPlan deactivate(long id) {
        return plans.save(findById(id).withActive(false));
    }

    /**
     * Allows selling a plan again.
     *
     * @param id the identifier of the plan
     * @return the updated plan
     * @throws ResourceNotFoundException when the plan does not exist
     */
    public MembershipPlan reactivate(long id) {
        return plans.save(findById(id).withActive(true));
    }

    private void ensureNameIsFree(String name, long ownId) {
        plans.findByName(name).filter(p -> p.getId() != ownId).ifPresent(p -> {
            throw new BusinessRuleException("Ya existe un plan con el nombre '" + name.trim() + "'");
        });
    }
}
