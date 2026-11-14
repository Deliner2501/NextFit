package com.nextfit.model;

/**
 * The numeric description of one face of a member (a vector of numbers, never a photo).
 * Several templates can belong to the same member.
 *
 * @param memberId the member the face belongs to
 * @param vector   the numbers that describe the face
 */
public record FaceTemplate(long memberId, float[] vector) {

    public FaceTemplate {
        vector = vector.clone();
    }

    /** @return a copy of the vector, so nobody can change the stored template */
    @Override
    public float[] vector() {
        return vector.clone();
    }
}
