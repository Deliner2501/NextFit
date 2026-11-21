package com.nextfit.persistence.jdbc;

import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.Exercise;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.model.TrainingSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Trainers, exercises, routines, assignments and sessions on an in-memory H2 database. */
class JdbcTrainingRepositoriesTest {

    private H2Database db;
    private long memberId;
    private Trainer trainer;
    private Exercise exercise;
    private JdbcRoutineRepository routines;

    @BeforeEach
    void setUp() {
        db = new H2Database();
        memberId = new JdbcMemberRepository(db).save(TestData.member("100")).getId();
        trainer = new JdbcTrainerRepository(db).save(TestData.trainer("T1"));
        exercise = new JdbcExerciseRepository(db).save(TestData.exercise("Press de banca"));
        routines = new JdbcRoutineRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void trainersAreFoundByDocumentAndExercisesByNameIgnoringCase() {
        assertEquals(trainer.getId(), new JdbcTrainerRepository(db).findByDocumentNumber("T1").orElseThrow().getId());
        assertTrue(new JdbcTrainerRepository(db).findByDocumentNumber("nadie").isEmpty());
        assertEquals(exercise.getId(), new JdbcExerciseRepository(db).findByName("PRESS DE BANCA").orElseThrow().getId());
        assertEquals("", new JdbcExerciseRepository(db).findById(exercise.getId()).orElseThrow().getImagePath());
    }

    @Test
    void aRoutineKeepsItsDaysItsExercisesAndTheirOrder() {
        Routine saved = routines.save(new Routine(0, "Fuerza", "ganar fuerza", trainer.getId(),
                LocalDate.of(2026, 11, 1), List.of(
                new RoutineDay("Lunes", List.of(new RoutineExercise(exercise.getId(), 3, 12, 60),
                        new RoutineExercise(exercise.getId(), 4, 10, 90))),
                new RoutineDay("Martes", List.of(new RoutineExercise(exercise.getId(), 5, 5, 120))))));

        Routine read = routines.findById(saved.getId()).orElseThrow();

        assertEquals(2, read.getDays().size());
        assertEquals("Lunes", read.getDays().get(0).getName());
        assertEquals(2, read.getDays().get(0).getExercises().size());
        assertEquals(3, read.getDays().get(0).getExercises().get(0).getSets());
        assertEquals(4, read.getDays().get(0).getExercises().get(1).getSets());
        assertEquals(120, read.getDays().get(1).getExercises().get(0).getRestSeconds());
    }

    @Test
    void savingARoutineAgainReplacesItsLinesInsteadOfAddingToThem() {
        Routine saved = routines.save(new Routine(0, "Fuerza", "obj", trainer.getId(), LocalDate.of(2026, 11, 1),
                List.of(new RoutineDay("Lunes", List.of(new RoutineExercise(exercise.getId(), 3, 12, 60),
                        new RoutineExercise(exercise.getId(), 4, 10, 90))))));

        routines.save(new Routine(saved.getId(), "Fuerza", "obj", trainer.getId(), LocalDate.of(2026, 11, 1),
                List.of(new RoutineDay("Lunes", List.of(new RoutineExercise(exercise.getId(), 2, 15, 45))))));

        Routine read = routines.findById(saved.getId()).orElseThrow();
        assertEquals(1, routines.findAll().size());
        assertEquals(1, read.getDays().get(0).getExercises().size());
        assertEquals(2, read.getDays().get(0).getExercises().get(0).getSets());
    }

    @Test
    void routinesAreFoundByTrainerAndARoutineWithoutTrainerComesBackWithZero() {
        routines.save(new Routine(0, "De Ana", "", trainer.getId(), LocalDate.of(2026, 11, 1), List.of()));
        Routine orphan = routines.save(new Routine(0, "Sin entrenador", "", 0, LocalDate.of(2026, 11, 1), List.of()));

        assertEquals(1, routines.findByTrainerId(trainer.getId()).size());
        assertEquals(0L, routines.findById(orphan.getId()).orElseThrow().getTrainerId());
        assertEquals("", routines.findById(orphan.getId()).orElseThrow().getObjective());
    }

    @Test
    void aRoutineThatFailsHalfWayIsNotLeftWithoutItsLines() {
        Routine saved = routines.save(new Routine(0, "Fuerza", "obj", trainer.getId(), LocalDate.of(2026, 11, 1),
                List.of(new RoutineDay("Lunes", List.of(new RoutineExercise(exercise.getId(), 3, 12, 60))))));

        try {
            // exercise 9999 does not exist: the line is refused after the old lines were deleted
            routines.save(new Routine(saved.getId(), "Fuerza", "obj", trainer.getId(), LocalDate.of(2026, 11, 1),
                    List.of(new RoutineDay("Lunes", List.of(new RoutineExercise(9999, 3, 12, 60))))));
        } catch (RuntimeException expected) {
            // the transaction must have been rolled back
        }

        Routine read = routines.findById(saved.getId()).orElseThrow();
        assertEquals(1, read.getDays().size());
        assertEquals(exercise.getId(), read.getDays().get(0).getExercises().get(0).getExerciseId());
    }

    @Test
    void assignmentsAndSessionsAreFoundByMember() {
        Routine routine = routines.save(new Routine(0, "Fuerza", "", trainer.getId(), LocalDate.of(2026, 11, 1), List.of()));
        JdbcAssignmentRepository assignments = new JdbcAssignmentRepository(db);
        RoutineAssignment assignment = assignments.save(new RoutineAssignment(0, memberId, routine.getId(),
                LocalDate.of(2026, 11, 2), AssignmentStatus.values()[0]));

        JdbcTrainingSessionRepository sessions = new JdbcTrainingSessionRepository(db);
        TrainingSession session = sessions.save(new TrainingSession(0, memberId, assignment.getId(), "Lunes",
                LocalDate.of(2026, 11, 3), 60, ""));

        assertEquals(1, assignments.findByMemberId(memberId).size());
        assertEquals(assignment.getId(), assignments.findById(assignment.getId()).orElseThrow().getId());
        assertEquals(1, sessions.findByMemberId(memberId).size());
        assertEquals(60, sessions.findById(session.getId()).orElseThrow().getDurationMinutes());
        assertEquals("", sessions.findById(session.getId()).orElseThrow().getNotes());
    }
}
