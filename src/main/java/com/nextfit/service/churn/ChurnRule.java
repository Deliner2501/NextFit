package com.nextfit.service.churn;

import java.util.Optional;

/**
 * One way of finding a sign that a member may stop coming. {@link ChurnService}
 * asks every rule it has, so a new sign is a new class that implements this
 * interface and the service does not change.
 */
@FunctionalInterface
public interface ChurnRule {

    /**
     * Looks at a member and says how much risk this rule sees.
     *
     * @param activity what is known about the member
     * @return the points and the reason, or empty when the rule sees nothing wrong
     */
    Optional<ChurnFactor> assess(MemberActivity activity);
}