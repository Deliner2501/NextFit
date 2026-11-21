package com.nextfit.service.face;

import com.nextfit.model.Member;

import java.time.LocalDate;

/** Small helpers shared by the face tests. */
final class FaceTestData {

    private FaceTestData() {
    }

    static Member member(long id, String name, boolean active) {
        return new Member(id, name, "10" + id, "300000000" + id, "m" + id + "@example.com",
                LocalDate.of(1998, 1, 1), LocalDate.of(2026, 1, 1), "", "Mamá 3110000000", active);
    }

    /** A vector that points to one direction; vectors with the same axis look alike. */
    static float[] face(double... values) {
        float[] vector = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            vector[i] = (float) values[i];
        }
        return vector;
    }
}
