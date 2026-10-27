package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.Member;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.persistence.CsvAssignmentRepository;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvRoutineRepository;
import com.nextfit.persistence.CsvTrainerRepository;
import com.nextfit.validation.ExerciseValidator;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.RoutineValidator;
import com.nextfit.validation.TrainerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the assignment rules, using real CSV files in a temporary folder. */
class RoutineAssignmentServiceTest {

    private RoutineAssignmentService service;
    private MemberService members;
    private long routineId;
    // Membresía falsa: un solo método, por eso se puede cambiar con una variable.
    private boolean hasMembership;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        hasMembership = true;
        SubscriptionChecker subscriptions = memberId -> hasMembership;

        members = new MemberService(
                new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        TrainerService trainers = new TrainerService(
                new CsvTrainerRepository(folder.resolve("trainers.csv")), new TrainerValidator());
        ExerciseService exercises = new ExerciseService(
                new CsvExerciseRepository(folder.resolve("exercises.csv")), new ExerciseValidator());
        RoutineService routines = new RoutineService(
                new CsvRoutineRepository(folder.resolve("routines.csv"), folder.resolve("lines.csv")),
                new RoutineValidator(), trainers, exercises);
        service = new RoutineAssignmentService(
                new CsvAssignmentRepository(folder.resolve("assignments.csv")),
                members, routines, subscriptions);

        // Datos base: un entrenador, un ejercicio y una rutina que los usa.
        Trainer trainer = trainers.register(new Trainer(0, "Carlos Pérez", "10203040", "3001234567",
                "", LocalDate.of(1990, 5, 10), "Hipertrofia", "", true));
        Exercise exercise = exercises.create(new Exercise(0, "Press de banca", "Descripción",
                MuscleGroup.CHEST, Difficulty.BEGINNER, ""));
        RoutineDay day = new RoutineDay("Día 1", List.of(new RoutineExercise(exercise.getId(), 3, 10, 60)));
        routineId = routines.create(new Routine(0, "Rutina básica", "Fuerza", trainer.getId(),
                LocalDate.now(), List.of(day))).getId();
    }

    // Registra un socio activo con el documento indicado y devuelve su id.
    private long newMember(String document) {
        return members.register(new Member(0, "Ana Gómez", document, "3105551234", "",
                LocalDate.of(2000, 1, 1), LocalDate.now(), "", "", true)).getId();
    }

    @Test
    void assignCreatesAnActiveAssignment() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = service.assign(memberId, routineId);
        assertEquals(AssignmentStatus.ACTIVE, assignment.getStatus());
        assertTrue(service.findCurrentFor(memberId).isPresent());
    }

    @Test
    void assignRejectsSecondActiveRoutine() {
        long memberId = newMember("11111111");
        service.assign(memberId, routineId);
        assertThrows(BusinessRuleException.class, () -> service.assign(memberId, routineId));
    }

    @Test
    void assignRejectsMemberWithoutMembership() {
        long memberId = newMember("11111111");
        hasMembership = false;
        assertThrows(BusinessRuleException.class, () -> service.assign(memberId, routineId));
    }

    @Test
    void assignRejectsInactiveMember() {
        long memberId = newMember("11111111");
        members.deactivate(memberId);
        assertThrows(BusinessRuleException.class, () -> service.assign(memberId, routineId));
    }

    @Test
    void assignFailsWhenMemberOrRoutineDoNotExist() {
        long memberId = newMember("11111111");
        assertThrows(ResourceNotFoundException.class, () -> service.assign(999, routineId));
        assertThrows(ResourceNotFoundException.class, () -> service.assign(memberId, 999));
    }

    @Test
    void cancelAllowsANewAssignment() {
        long memberId = newMember("11111111");
        RoutineAssignment first = service.assign(memberId, routineId);
        service.cancel(first.getId());
        assertFalse(service.findCurrentFor(memberId).isPresent());
        service.assign(memberId, routineId);   // ya se puede asignar otra
        assertEquals(2, service.findHistoryFor(memberId).size());
    }

    @Test
    void closedAssignmentCannotBeClosedAgain() {
        long memberId = newMember("11111111");
        RoutineAssignment first = service.assign(memberId, routineId);
        service.complete(first.getId());
        assertThrows(BusinessRuleException.class, () -> service.cancel(first.getId()));
    }

    @Test
    void findCurrentForIsEmptyForAMemberWithoutAssignments() {
        assertFalse(service.findCurrentFor(newMember("22222222")).isPresent());
    }
}