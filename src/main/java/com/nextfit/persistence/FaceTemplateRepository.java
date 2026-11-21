package com.nextfit.persistence;

import com.nextfit.model.FaceTemplate;

import java.util.List;

/** Storage of the face templates of the members. */
public interface FaceTemplateRepository {

    /** @return every stored template */
    List<FaceTemplate> findAll();

    /**
     * Replaces every template of the member with the given vectors.
     *
     * @param memberId the member
     * @param vectors  the new vectors
     */
    void replace(long memberId, List<float[]> vectors);

    /**
     * Removes every template of the member; nothing happens when it has none.
     *
     * @param memberId the member
     */
    void delete(long memberId);

    /**
     * @param memberId the member
     * @return true when the member has at least one template
     */
    default boolean has(long memberId) {
        return findAll().stream().anyMatch(t -> t.memberId() == memberId);
    }
}
