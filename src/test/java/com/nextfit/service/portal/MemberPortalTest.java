package com.nextfit.service.portal;

import com.nextfit.TrainingModule;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
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
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.service.AccessService;
import com.nextfit.service.AccessTokens;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.PhoneAccess;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.gamification.GamificationService;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.web.AccessServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Uses the real services with CSV files in a temporary folder, like a phone would find them. */
class MemberPortalTest {

    private static final Clock CLOCK = Clock.systemDefaultZone();

    private MemberService members;
    private SubscriptionService subscriptions;
    private MembershipPlanService plans;
    private TrainingModule training;
    private PhoneAccess phone;
    private MemberPortal portal;
    private Member ana;
    private Routine routine;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        members = new MemberService(new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        subscriptions = new SubscriptionService(
                new CsvSubscriptionRepository(folder.resolve("subscriptions.csv")), plans, members, CLOCK);
        AttendanceService attendance = new AttendanceService(members,
                new CsvAttendanceRepository(folder.resolve("attendance.csv")), subscriptions, CLOCK);
        AccessService access = new AccessService(members, attendance, id -> Optional.empty(), CLOCK);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        phone = new PhoneAccess(members, access, new AccessTokens(key), CLOCK);
        training = new TrainingModule(folder, members, subscriptions);
        portal = new MemberPortal(phone, subscriptions, plans, training.assignments(), training.routines(),
                training.exercises(), training.sessions(), new GamificationService(members, attendance, CLOCK));
        ana = members.register(new Member(0, "Ana Gómez", "1065123456", "3001234567", "ana@example.com",
                LocalDate.of(1998, 3, 1), LocalDate.now(), "", "Mamá 3110000000", true));
    }

    private void subscribe() {
        MembershipPlan plan = plans.create(new MembershipPlan(0, "Mensual", "Acceso por un mes", 30, 80000, true));
        subscriptions.subscribe(ana.getId(), plan.getId(), LocalDate.now());
    }

    private void assignRoutine() {
        Trainer trainer = training.trainers().register(new Trainer(0, "Carlos Pérez", "20002", "3105551234",
                "carlos@example.com", LocalDate.of(1990, 5, 5), "Fuerza", "", true));
        Exercise squat = training.exercises().create(new Exercise(0, "Sentadilla", "Piernas con barra",
                MuscleGroup.LEGS, Difficulty.INTERMEDIATE, ""));
        Exercise bench = training.exercises().create(new Exercise(0, "Press de banca", "Pecho con barra",
                MuscleGroup.CHEST, Difficulty.INTERMEDIATE, ""));
        routine = training.routines().create(new Routine(0, "Fuerza base", "Ganar fuerza", trainer.getId(),
                LocalDate.now(), List.of(
                new RoutineDay("Pierna", List.of(new RoutineExercise(squat.getId(), 4, 8, 90))),
                new RoutineDay("Pecho", List.of(new RoutineExercise(bench.getId(), 3, 10, 60))))));
        training.assignments().assign(ana.getId(), routine.getId());
    }

    @Test
    void anUnknownCodeGetsNoSummary() {
        assertTrue(portal.summaryFor("ZZZZZZZZZZ").isEmpty());
    }

    @Test
    void aMemberWithNothingStillGetsASummary() {
        PortalSummary summary = portal.summaryFor(phone.tokenOf(ana)).orElseThrow();

        assertEquals("Ana Gómez", summary.member().getFullName());
        assertTrue(summary.membership().isEmpty());
        assertTrue(summary.workout().isEmpty());
        assertEquals(0, summary.achievements().trainingDays());
    }

    @Test
    void showsThePlanAndTheDaysLeft() {
        subscribe();

        PortalMembership membership = portal.summaryFor(phone.tokenOf(ana)).orElseThrow().membership().orElseThrow();

        assertEquals("Mensual", membership.planName());
        assertEquals(LocalDate.now().plusDays(29), membership.validUntil());
        assertEquals(29, membership.daysLeft());
    }

    @Test
    void showsTheFirstDayOfTheRoutineWhenNothingWasLogged() {
        subscribe();
        assignRoutine();

        PortalWorkout workout = portal.summaryFor(phone.tokenOf(ana)).orElseThrow().workout().orElseThrow();

        assertEquals("Fuerza base", workout.routineName());
        assertEquals("Pierna", workout.dayName());
        assertEquals(1, workout.dayNumber());
        assertEquals(2, workout.dayCount());
        assertEquals("Sentadilla", workout.exercises().get(0).name());
        assertEquals("Piernas", workout.exercises().get(0).muscleGroup());
        assertEquals(4, workout.exercises().get(0).sets());
    }

    @Test
    void movesToTheNextDayAfterALoggedSession() {
        subscribe();
        assignRoutine();
        RoutineAssignment assignment = training.assignments().findCurrentFor(ana.getId()).orElseThrow();
        training.sessions().log(new TrainingSession(0, ana.getId(), assignment.getId(), "Pierna",
                LocalDate.now(), 60, ""));

        PortalWorkout workout = portal.summaryFor(phone.tokenOf(ana)).orElseThrow().workout().orElseThrow();

        assertEquals("Pecho", workout.dayName());
        assertEquals(2, workout.dayNumber());
    }

    private HttpResponse<String> get(AccessServer server, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void thePhoneOpensThePortalWithTheCodeOfTheCarnet() throws Exception {
        subscribe();
        assignRoutine();
        AccessServer server = new AccessServer(phone, portal, 0);
        server.start();
        try {
            HttpResponse<String> page = get(server, "/m/" + phone.tokenOf(ana));
            HttpResponse<String> invalid = get(server, "/m/ZZZZZZZZZZ");

            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("Mensual"));
            assertTrue(page.body().contains("Sentadilla"));
            assertEquals(404, invalid.statusCode());
            assertTrue(invalid.body().contains("Código no válido"));
        } finally {
            server.stop();
        }
    }

    @Test
    void withoutAPortalTheRouteDoesNotExist() throws Exception {
        AccessServer server = new AccessServer(phone, 0);
        server.start();
        try {
            assertEquals(404, get(server, "/m/" + phone.tokenOf(ana)).statusCode());
        } finally {
            server.stop();
        }
    }
}
