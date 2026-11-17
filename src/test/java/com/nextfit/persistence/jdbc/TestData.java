package com.nextfit.persistence.jdbc;

import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Trainer;

import java.time.LocalDate;

/** Small factories of valid objects, so each test only says what is different. */
final class TestData {

    private TestData() {
    }

    static Member member(String document) {
        return new Member(0, "Socio " + document, document, "3000000000", "socio@nextfit.test",
                LocalDate.of(2000, 1, 1), LocalDate.of(2026, 1, 10), "", "", true);
    }

    static MembershipPlan plan(String name) {
        return new MembershipPlan(0, name, "descripcion", 30, 50_000L, true);
    }

    static Trainer trainer(String document) {
        return new Trainer(0, "Entrenador " + document, document, "3000000001", "t@nextfit.test",
                LocalDate.of(1990, 1, 1), "Fuerza", "", true);
    }

    static Exercise exercise(String name) {
        return new Exercise(0, name, "descripcion", MuscleGroup.values()[0], Difficulty.values()[0], "");
    }
}
