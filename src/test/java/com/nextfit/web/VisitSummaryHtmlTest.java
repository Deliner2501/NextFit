package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.AccessResult.Outcome;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.portal.PortalBestTimes;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.PortalWorkout;
import com.nextfit.service.portal.VisitSummary;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitSummaryHtmlTest {

    private static VisitSummary entry(PortalWorkout workout) {
        return new VisitSummary(VisitSummary.Kind.ENTRY, Optional.ofNullable(workout),
                Optional.of(new PortalMembership("Mensual", LocalDate.of(2026, 11, 20), 3)), Optional.empty(), 12, 3,
                Optional.of(new PortalBestTimes("Hoy", List.of(new HourForecast(10, 4.0, 20, OccupancyLevel.QUIET),
                        new HourForecast(15, 5.0, 25, OccupancyLevel.QUIET)))));
    }

    private static PortalWorkout workout(int exercises) {
        List<PortalExercise> lines = new ArrayList<>();
        for (int i = 1; i <= exercises; i++) {
            lines.add(new PortalExercise("Ejercicio " + i, "Piernas", 4, 10, 60));
        }
        return new PortalWorkout("Fuerza", "Pierna <b>", 2, 3, lines);
    }

    @Test
    void entryShowsYourDayWithTheRoutineTheMembershipAndTheBestHours() {
        String html = VisitSummaryHtml.render(entry(workout(2)));

        assertTrue(html.contains("Tu día"));
        assertTrue(html.contains("Te toca hoy"));
        assertTrue(html.contains("Ejercicio 1"));
        assertTrue(html.contains("quedan 3 días"));
        assertTrue(html.contains("10:00 · 15:00"));
        assertFalse(html.contains("Tu resumen"));
    }

    @Test
    void textFromTheTrainerIsEscaped() {
        String html = VisitSummaryHtml.render(entry(workout(1)));

        assertTrue(html.contains("Pierna &lt;b&gt;"));
        assertFalse(html.contains("Pierna <b>"));
    }

    @Test
    void longRoutinesShowOnlyTheFirstExercises() {
        String html = VisitSummaryHtml.render(entry(workout(9)));

        assertTrue(html.contains("Ejercicio 6"));
        assertFalse(html.contains("Ejercicio 7"));
        assertTrue(html.contains("y 3 más en tu rutina"));
    }

    @Test
    void entryWithoutARoutineAsksForOne() {
        assertTrue(VisitSummaryHtml.render(entry(null)).contains("Pídesela a tu entrenador"));
    }

    @Test
    void exitShowsTheStayTheProgressAndWhatComesNext() {
        VisitSummary exit = new VisitSummary(VisitSummary.Kind.EXIT, Optional.of(workout(2)), Optional.empty(),
                Optional.of(Duration.ofMinutes(75)), 12, 1, Optional.empty());

        String html = VisitSummaryHtml.render(exit);

        assertTrue(html.contains("Tu resumen"));
        assertTrue(html.contains("1 h 15 min"));
        assertTrue(html.contains("12"));
        assertTrue(html.contains("semana seguida"));
        assertTrue(html.contains("Lo que sigue"));
        assertFalse(html.contains("Ejercicio 1"));
    }

    @Test
    void thePageShowsTheSummaryOnlyWhenAccessIsGranted() {
        Member member = new Member(1, "Ana Gómez", "1065123456", "3001112233", "a@b.co", LocalDate.of(1998, 5, 1),
                LocalDate.of(2026, 1, 1), "", "Mamá 3001", true);
        AccessResult entry = new AccessResult(Outcome.ENTRY, member, "Bienvenido", LocalDate.of(2026, 11, 20), 6);
        AccessResult denied = new AccessResult(Outcome.DENIED, member, "Sin membresía", null, -1);

        assertTrue(AccessPage.render(entry, "ABCDEFGHJK", Optional.of(entry(workout(1)))).contains("Tu día"));
        assertFalse(AccessPage.render(denied, "ABCDEFGHJK", Optional.of(entry(workout(1)))).contains("Tu día"));
        assertFalse(AccessPage.render(entry, "ABCDEFGHJK").contains("Tu día"));
    }
}
