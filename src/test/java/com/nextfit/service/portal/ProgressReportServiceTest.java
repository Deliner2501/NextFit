package com.nextfit.service.portal;

import com.nextfit.TrainingModule;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.Trainer;
import com.nextfit.model.TrainingSession;
import com.nextfit.persistence.CsvAttendanceRepository;
import com.nextfit.persistence.CsvHealthProfileRepository;
import com.nextfit.persistence.CsvMeasurementRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressReportServiceTest {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private MemberService members;
    private TrainingModule training;
    private AttendanceService attendance;
    private HealthService health;
    private ProgressReportService service;
    private Member ana;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        members = new MemberService(new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        SubscriptionService subscriptions = new SubscriptionService(
                new CsvSubscriptionRepository(folder.resolve("subscriptions.csv")), plans, members, CLOCK);
        attendance = new AttendanceService(members, new CsvAttendanceRepository(folder.resolve("attendance.csv")),
                subscriptions, CLOCK);
        health = new HealthService(members, new CsvHealthProfileRepository(folder.resolve("profiles.csv")),
                new CsvMeasurementRepository(folder.resolve("measurements.csv")), new HealthValidator());
        training = new TrainingModule(folder, members, subscriptions);
        service = new ProgressReportService(training.assignments(), training.routines(), training.sessions(),
                attendance, health);
        ana = members.register(new Member(0, "Ana Gómez", "1065123456", "3001234567", "ana@example.com",
                LocalDate.of(1998, 3, 1), LocalDate.now(), "", "Mamá 3110000000", true));
        MembershipPlan plan = plans.create(new MembershipPlan(0, "Mensual", "Acceso por un mes", 30, 80000, true));
        subscriptions.subscribe(ana.getId(), plan.getId(), LocalDate.now());
    }

    private RoutineAssignment assignThreeDayRoutine() {
        Trainer trainer = training.trainers().register(new Trainer(0, "Carlos Pérez", "20002", "3105551234",
                "carlos@example.com", LocalDate.of(1990, 5, 5), "Fuerza", "", true));
        Exercise squat = training.exercises().create(new Exercise(0, "Sentadilla", "Piernas con barra",
                MuscleGroup.LEGS, Difficulty.INTERMEDIATE, ""));
        Routine routine = training.routines().create(new Routine(0, "Fuerza base", "Ganar fuerza", trainer.getId(),
                LocalDate.now(), List.of(
                new RoutineDay("Pierna", List.of(new RoutineExercise(squat.getId(), 4, 8, 90))),
                new RoutineDay("Pecho", List.of(new RoutineExercise(squat.getId(), 3, 10, 60),
                        new RoutineExercise(squat.getId(), 3, 10, 60))),
                new RoutineDay("Espalda", List.of(new RoutineExercise(squat.getId(), 3, 12, 60))))));
        return training.assignments().assign(ana.getId(), routine.getId());
    }

    @Test
    void aMemberWithNothingGetsAnEmptyReport() {
        ProgressReport report = service.of(ana);

        assertTrue(report.upcoming().isEmpty());
        assertTrue(report.visits().isEmpty());
        assertTrue(report.body().isEmpty());
    }

    @Test
    void upcomingDaysStartAfterTheDayToTrainToday() {
        assignThreeDayRoutine();

        List<ProgressReport.UpcomingDay> upcoming = service.of(ana).upcoming();

        assertEquals(2, upcoming.size());
        assertEquals("Pecho", upcoming.get(0).name());
        assertEquals(2, upcoming.get(0).dayNumber());
        assertEquals(2, upcoming.get(0).exerciseCount());
        assertEquals("Espalda", upcoming.get(1).name());
    }

    @Test
    void upcomingDaysGoAroundTheRoutineAfterALoggedSession() {
        RoutineAssignment assignment = assignThreeDayRoutine();
        training.sessions().log(new TrainingSession(0, ana.getId(), assignment.getId(), "Pierna",
                LocalDate.now(), 60, ""));

        List<ProgressReport.UpcomingDay> upcoming = service.of(ana).upcoming();

        assertEquals("Espalda", upcoming.get(0).name());
        assertEquals("Pierna", upcoming.get(1).name());
    }

    @Test
    void visitsAreTheLastFiveNewestFirstWithTheStayWhenTheMemberLeft() {
        for (int i = 0; i < 6; i++) {
            attendance.checkIn(ana.getId());
            attendance.checkOut(ana.getId());
        }
        attendance.checkIn(ana.getId());

        List<ProgressReport.RecentVisit> visits = service.of(ana).visits();

        assertEquals(5, visits.size());
        assertTrue(visits.get(0).stay().isEmpty());
        assertTrue(visits.get(1).stay().isPresent());
        assertFalse(visits.get(1).stay().orElseThrow().isNegative());
    }

    @Test
    void bodyProgressCompaaresTheFirstAndTheLastMeasure() {
        health.saveProfile(new HealthProfile(ana.getId(), 170, "O+", "", ""));
        health.addMeasurement(new Measurement(0, ana.getId(), LocalDate.now().minusDays(60), 80, 90, 100, 100));
        health.addMeasurement(new Measurement(0, ana.getId(), LocalDate.now(), 74, 85, 98, 98));

        ProgressReport.BodyProgress body = service.of(ana).body().orElseThrow();

        assertEquals(80, body.firstWeightKg(), 0.001);
        assertEquals(74, body.latestWeightKg(), 0.001);
        assertEquals(2, body.measures());
        assertEquals(LocalDate.now().minusDays(60), body.since());
        assertTrue(body.bmi().isPresent());
    }
}
