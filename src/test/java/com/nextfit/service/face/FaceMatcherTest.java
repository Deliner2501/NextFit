package com.nextfit.service.face;

import com.nextfit.model.FaceTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.nextfit.service.face.FaceTestData.face;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceMatcherTest {

    private final List<FaceTemplate> templates = List.of(
            new FaceTemplate(1, face(1, 0, 0)),
            new FaceTemplate(1, face(0.9, 0.1, 0)),
            new FaceTemplate(2, face(0, 1, 0)));

    @Test
    void cosineOfEqualVectorsIsOneAndOfPerpendicularIsZero() {
        assertEquals(1.0, FaceMatcher.cosine(face(1, 2, 3), face(2, 4, 6)), 1e-9);
        assertEquals(0.0, FaceMatcher.cosine(face(1, 0), face(0, 1)), 1e-9);
        assertEquals(-1.0, FaceMatcher.cosine(face(1, 0), face(-1, 0)), 1e-9);
    }

    @Test
    void zeroVectorsAndDifferentSizes() {
        assertEquals(0.0, FaceMatcher.cosine(face(0, 0), face(1, 1)), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> FaceMatcher.cosine(face(1), face(1, 2)));
    }

    @Test
    void findsTheMemberWhoseFaceLooksMost() {
        Optional<FaceMatcher.Match> match = FaceMatcher.best(face(0.95, 0.05, 0), templates, 0.45, 0.05);
        assertTrue(match.isPresent());
        assertEquals(1, match.get().memberId());
        assertEquals(2, FaceMatcher.best(face(0.05, 1, 0), templates, 0.45, 0.05).orElseThrow().memberId());
    }

    @Test
    void refusesAFaceThatLooksLikeNobody() {
        assertTrue(FaceMatcher.best(face(0, 0, 1), templates, 0.45, 0.05).isEmpty());
    }

    @Test
    void refusesWhenTwoMembersAreAlmostEquallySimilar() {
        assertTrue(FaceMatcher.best(face(1, 1, 0), templates, 0.45, 0.1).isEmpty());
        assertTrue(FaceMatcher.best(face(1, 1, 0), templates, 0.45, 0.05).isPresent());
    }

    @Test
    void nothingStoredMeansNoMatch() {
        assertTrue(FaceMatcher.best(face(1, 0, 0), List.of(), 0.45, 0.05).isEmpty());
    }

    @Test
    void severalTemplatesOfTheSameMemberDoNotCountAsASecondCandidate() {
        List<FaceTemplate> same = List.of(new FaceTemplate(7, face(1, 0)), new FaceTemplate(7, face(0.99, 0.01)));
        assertEquals(7, FaceMatcher.best(face(1, 0), same, 0.45, 0.05).orElseThrow().memberId());
    }
}
