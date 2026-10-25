package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.Member;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.model.TrainingSession;
import com.nextfit.persistence.CsvAssignmentRepository;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvRoutineRepository;
import com.nextfit.persistence.CsvTrainerRepository;
import com.nextfit.persistence.CsvTrainingSessionRepository;
import com.nextfit.validation.ExerciseValidator;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.RoutineValidator;
import com.nextfit.validation.TrainerValidator;
import com.nextfit.validation.TrainingSessionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests of the session logging rules, using real CSV files in a temporary folder. */
class TrainingSessionServiceTest {

    private static final String DAY_NAME = "Día 1";

    private TrainingSessionService service;
    private RoutineAssignmentService assignments;
    private MemberService members;
    private long routineId;
    private boolean hasMembership;   // membresía falsa, se cambia desde cada prueba

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
        assignments = new RoutineAssignmentService(
                new CsvAssignmentRepository(folder.resolve("assignments.csv")),
                members, routines, subscriptions);
        service = new TrainingSessionService(
                new CsvTrainingSessionRepository(folder.resolve("sessions.csv")),
                new TrainingSessionValidator(), assignments, routines, subscriptions);

        // Datos base: un entrenador, un ejercicio y una rutina con un día llamado "Día 1".
        Trainer trainer = trainers.register(new Trainer(0, "Carlos Pérez", "10203040", "3001234567",
                "", LocalDate.of(1990, 5, 10), "Hipertrofia", "", true));
        Exercise exercise = exercises.create(new Exercise(0, "Press de banca", "Descripción",
                MuscleGroup.CHEST, Difficulty.BEGINNER, ""));
        RoutineDay day = new RoutineDay(DAY_NAME, List.of(new RoutineExercise(exercise.getId(), 3, 10, 60)));
        routineId = routines.create(new Routine(0, "Rutina básica", "Fuerza", trainer.getId(),
                LocalDate.now(), List.of(day))).getId();
    }

    // Registra un socio con el documento indicado y devuelve su id.
    private long newMember(String document) {
        return members.register(new Member(0, "Ana Gómez", document, "3105551234", "",
                LocalDate.of(2000, 1, 1), LocalDate.now(), "", "", true)).getId();
    }

    // Crea una sesión de prueba (id 0, 45 minutos) para el socio y la asignación indicados.
    private TrainingSession session(long memberId, long assignmentId, String dayName, LocalDate date) {
        return new TrainingSession(0, memberId, assignmentId, dayName, date, 45, "");
    }

    @Test
    void logStoresASessionOfAnActiveAssignment() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = assignments.assign(memberId, routineId);

        TrainingSession saved = service.log(session(memberId, assignment.getId(), DAY_NAME, LocalDate.now()));

        assertEquals(1, saved.getId());
    }

    @Test
    void logRejectsMemberWithoutMembership() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = assignments.assign(memberId, routineId);
        hasMembership = false;
        assertThrows(BusinessRuleException.class,
                () -> service.log(session(memberId, assignment.getId(), DAY_NAME, LocalDate.now())));
    }

    @Test
    void logRejectsAssignmentOfAnotherMember() {
        long owner = newMember("11111111");
        long other = newMember("22222222");
        RoutineAssignment assignment = assignments.assign(owner, routineId);
        assertThrows(BusinessRuleException.class,
                () -> service.log(session(other, assignment.getId(), DAY_NAME, LocalDate.now())));
    }

    @Test
    void logRejectsAnAssignmentThatIsNoLongerActive() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = assignments.assign(memberId, routineId);
        assignments.cancel(assignment.getId());
        assertThrows(BusinessRuleException.class,
                () -> service.log(session(memberId, assignment.getId(), DAY_NAME, LocalDate.now())));
    }

    @Test
    void logRejectsADayThatIsNotInTheRoutine() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = assignments.assign(memberId, routineId);
        assertThrows(BusinessRuleException.class,
                () -> service.log(session(memberId, assignment.getId(), "Día 9", LocalDate.now())));
    }

    @Test
    void logRejectsAFutureDate() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = assignments.assign(memberId, routineId);
        assertThrows(InvalidDataException.class, () -> service.log(
                session(memberId, assignment.getId(), DAY_NAME, LocalDate.now().plusDays(1))));
    }

    @Test
    void findByMemberReturnsNewestFirst() {
        long memberId = newMember("11111111");
        RoutineAssignment assignment = assignments.assign(memberId, routineId);
        service.log(session(memberId, assignment.getId(), DAY_NAME, LocalDate.now().minusDays(3)));
        service.log(session(memberId, assignment.getId(), DAY_NAME, LocalDate.now()));

        List<TrainingSession> result = service.findByMember(memberId);

        assertEquals(2, result.size());
        assertEquals(LocalDate.now(), result.get(0).getDate());
    }
}