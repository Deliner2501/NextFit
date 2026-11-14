package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Member;
import com.nextfit.service.gamification.Achievements;
import com.nextfit.service.gamification.Streak;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.portal.PortalBestTimes;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.PortalSummary;
import com.nextfit.service.portal.PortalWorkout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the tools of the member: what they say, and that nobody can read the data of another member. */
class MemberToolsTest {

    private static final AiUser ANA = AiUser.member(7, "Ana");
    private static final AiUser ADMIN = new AiUser(AiAudience.ADMIN, 0, "admin");

    private final Map<Long, PortalSummary> summaries = new HashMap<>();
    private final List<Long> asked = new ArrayList<>();
    private MemberSummaryLookup lookup;

    @BeforeEach
    void setUp() {
        lookup = memberId -> {
            asked.add(memberId);
            return Optional.ofNullable(summaries.get(memberId));
        };
    }

    private static Member member(long id, String name) {
        return new Member(id, name, "1000" + id, "3001234567", "socio@example.com", LocalDate.of(1998, 3, 1),
                LocalDate.of(2026, 1, 5), "", "Mamá 3110000000", true);
    }

    private void put(long id, String name, Optional<PortalMembership> membership, Optional<PortalWorkout> workout,
                     Optional<PortalBestTimes> bestTimes) {
        Member member = member(id, name);
        summaries.put(id, new PortalSummary(member, membership, workout,
                new Achievements(member, 0, new Streak(0, 0), List.of()), bestTimes));
    }

    private static PortalMembership monthly(long daysLeft) {
        return new PortalMembership("Mensual", LocalDate.of(2026, 12, 14), daysLeft);
    }

    // ---- membership ----

    @Test
    void theMembershipSaysThePlanTheDateAndTheDaysLeft() {
        put(7, "Ana Gómez", Optional.of(monthly(33)), Optional.empty(), Optional.empty());

        String text = new MyMembershipTool(lookup).run(Map.of(), ANA);

        assertEquals("Plan: Mensual. Vigente hasta el 14 de diciembre 2026. Le quedan 33 día(s).", text);
    }

    @Test
    void aMembershipThatEndsTodaySaysSo() {
        put(7, "Ana Gómez", Optional.of(monthly(0)), Optional.empty(), Optional.empty());

        assertTrue(new MyMembershipTool(lookup).run(Map.of(), ANA).endsWith("Vence hoy."));
    }

    @Test
    void withoutMembershipItSaysHowToRenew() {
        put(7, "Ana Gómez", Optional.empty(), Optional.empty(), Optional.empty());

        assertTrue(new MyMembershipTool(lookup).run(Map.of(), ANA).contains("No tiene una membresía vigente"));
    }

    // ---- next workout ----

    @Test
    void theWorkoutListsTheExercisesOfTheDay() {
        PortalWorkout workout = new PortalWorkout("Fuerza", "Pierna", 3, 3,
                List.of(new PortalExercise("Sentadilla", "Piernas", 4, 8, 90)));
        put(7, "Ana Gómez", Optional.empty(), Optional.of(workout), Optional.empty());

        String text = new MyNextWorkoutTool(lookup).run(Map.of(), ANA);

        assertTrue(text.startsWith("Le toca: Pierna (rutina Fuerza, día 3 de 3)."));
        assertTrue(text.contains("- Sentadilla (Piernas): 4 series de 8 repeticiones, descanso 90 s"));
    }

    @Test
    void aDayWithoutExercisesSaysSo() {
        put(7, "Ana Gómez", Optional.empty(),
                Optional.of(new PortalWorkout("Fuerza", "Pierna", 1, 3, List.of())), Optional.empty());

        assertTrue(new MyNextWorkoutTool(lookup).run(Map.of(), ANA).contains("no tiene ejercicios todavía"));
    }

    @Test
    void withoutRoutineItSaysToAskTheTrainer() {
        put(7, "Ana Gómez", Optional.empty(), Optional.empty(), Optional.empty());

        assertTrue(new MyNextWorkoutTool(lookup).run(Map.of(), ANA).contains("No tiene una rutina asignada"));
    }

    // ---- best hours ----

    @Test
    void theBestHoursAreListedWithTheirLevel() {
        PortalBestTimes times = new PortalBestTimes("Hoy", List.of(
                new HourForecast(17, 0.0, 0, OccupancyLevel.QUIET),
                new HourForecast(19, 40.0, 67, OccupancyLevel.BUSY)));
        put(7, "Ana Gómez", Optional.empty(), Optional.empty(), Optional.of(times));

        String text = new BestHoursTool(lookup).run(Map.of(), ANA);

        assertEquals("Hoy el gimnasio estará más tranquilo a las: 17:00 (Tranquilo), 19:00 (Concurrido).", text);
    }

    @Test
    void withoutHistoryItDoesNotInventAnHour() {
        put(7, "Ana Gómez", Optional.empty(), Optional.empty(), Optional.empty());

        assertTrue(new BestHoursTool(lookup).run(Map.of(), ANA).contains("no hay suficientes visitas"));
    }

    // ---- privacy ----

    @Test
    void aMemberOnlyGetsHisOwnData() {
        put(7, "Ana Gómez", Optional.of(monthly(33)), Optional.empty(), Optional.empty());
        put(8, "Luis Pérez", Optional.of(new PortalMembership("Anual", LocalDate.of(2027, 5, 1), 200)),
                Optional.empty(), Optional.empty());

        String text = new MyMembershipTool(lookup).run(Map.of(), ANA);

        assertTrue(text.contains("Mensual"));
        assertFalse(text.contains("Anual"));
    }

    @Test
    void anArgumentThatNamesAnotherMemberChangesNothing() {
        put(7, "Ana Gómez", Optional.of(monthly(33)), Optional.empty(), Optional.empty());
        put(8, "Luis Pérez", Optional.of(new PortalMembership("Anual", LocalDate.of(2027, 5, 1), 200)),
                Optional.empty(), Optional.empty());

        String text = new MyMembershipTool(lookup).run(Map.of("memberId", 8L, "member_id", 8, "name", "Luis"), ANA);

        assertTrue(text.contains("Mensual"));
        assertEquals(List.of(7L), asked);
    }

    @Test
    void aPersonWhoIsNotAMemberIsRefusedAndNothingIsRead() {
        assertThrows(BusinessRuleException.class, () -> new MyMembershipTool(lookup).run(Map.of(), ADMIN));
        assertThrows(BusinessRuleException.class,
                () -> new MyNextWorkoutTool(lookup).run(Map.of(), new AiUser(AiAudience.MEMBER, 0, "sin id")));

        assertTrue(asked.isEmpty());
    }

    @Test
    void aMemberThatDoesNotExistGetsAClearError() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> new BestHoursTool(lookup).run(Map.of(), AiUser.member(99, "Nadie")));

        assertEquals("No se encontró al socio", error.getMessage());
    }

    @Test
    void onlyTheMemberMayUseTheseTools() {
        List<com.nextfit.ai.AiTool> tools = List.of(
                new MyMembershipTool(lookup), new MyNextWorkoutTool(lookup), new BestHoursTool(lookup));

        for (com.nextfit.ai.AiTool tool : tools) {
            assertTrue(tool.allowedFor(AiAudience.MEMBER));
            assertFalse(tool.allowedFor(AiAudience.ADMIN));
            assertFalse(tool.allowedFor(AiAudience.RECEPTION));
            assertFalse(tool.allowedFor(AiAudience.TRAINER));
        }
    }
}