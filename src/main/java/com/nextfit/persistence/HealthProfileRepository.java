package com.nextfit.persistence;

import com.nextfit.model.HealthProfile;

import java.util.Optional;

/**
 * Storage of health profiles. A member has at most one profile. Services
 * depend on this interface, so the CSV implementation can be replaced by a
 * database one without changing them.
 */
public interface HealthProfileRepository {

    /**
     * Looks the health profile of a member up.
     *
     * @param memberId the identifier of the member
     * @return the profile, or empty when the member has none yet
     */
    Optional<HealthProfile> findByMemberId(long memberId);

    /**
     * Inserts the profile when the member has none, or replaces the one the
     * member already has.
     *
     * @param profile the profile to store
     * @return the stored profile
     */
    HealthProfile save(HealthProfile profile);
}