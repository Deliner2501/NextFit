package com.nextfit.service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Tells until which day a member's current membership is valid. The access
 * service depends on this small contract, not on the membership module.
 */
@FunctionalInterface
public interface MembershipExpiry {

    /**
     * Returns the last valid day of the current membership.
     *
     * @param memberId the identifier of the member
     * @return the end date, or empty when the member has no valid membership today
     */
    Optional<LocalDate> validUntil(long memberId);
}
