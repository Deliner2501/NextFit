package com.nextfit.service.face;

import com.nextfit.model.FaceTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Decides which member a face belongs to by comparing vectors with the cosine similarity. */
public final class FaceMatcher {

    private FaceMatcher() {
    }

    /**
     * The member a face was recognized as.
     *
     * @param memberId the member
     * @param score    the similarity, from -1 to 1 (1 is identical)
     */
    public record Match(long memberId, double score) {
    }

    /**
     * Cosine similarity of two vectors.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the similarity, or 0 when one of them is all zeros
     * @throws IllegalArgumentException when the vectors have different sizes
     */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Los vectores deben tener el mismo tamaño");
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Finds the member whose face looks most like the probe. It answers nothing when the best
     * similarity is under the threshold, or when a second member is almost as similar (too risky).
     *
     * @param probe     the vector of the face in front of the camera
     * @param templates the stored templates
     * @param threshold the minimum similarity to accept
     * @param margin    how much better than the second member the best one must be
     * @return the match, or empty when the face is not recognized with confidence
     */
    public static Optional<Match> best(float[] probe, List<FaceTemplate> templates, double threshold, double margin) {
        Map<Long, Double> bestPerMember = new HashMap<>();
        for (FaceTemplate template : templates) {
            double score = cosine(probe, template.vector());
            bestPerMember.merge(template.memberId(), score, Math::max);
        }
        Match first = null;
        double secondScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Long, Double> entry : bestPerMember.entrySet()) {
            if (first == null || entry.getValue() > first.score()) {
                if (first != null) {
                    secondScore = Math.max(secondScore, first.score());
                }
                first = new Match(entry.getKey(), entry.getValue());
            } else {
                secondScore = Math.max(secondScore, entry.getValue());
            }
        }
        if (first == null || first.score() < threshold || first.score() - secondScore < margin) {
            return Optional.empty();
        }
        return Optional.of(first);
    }
}
