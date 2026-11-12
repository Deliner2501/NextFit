package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.gamification.Achievements;
import com.nextfit.service.gamification.Badge;
import com.nextfit.service.gamification.Streak;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.PortalSummary;
import com.nextfit.service.portal.PortalWorkout;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalPageTest {

    private static Member member(String name) {
        return new Member(1, name, "10001", "3001234567", "a@b.co", LocalDate.of(2000, 1, 1),
                LocalDate.of(2026, 1, 1), "", "Mamá 3110000000", true);
    }

    private static PortalSummary summary(Member member, Optional<PortalMembership> membership,
                                         Optional<PortalWorkout> workout, Achievements achievements) {
        return new PortalSummary(member, membership, workout, achievements);
    }

    private static Achievements nothingYet() {
        return new Achievements(member("Ana Gómez"), 0, new Streak(0, 0), List.of());
    }

    @Test
    void showsTheMembershipTheWorkoutAndTheProgress() {
        PortalWorkout workout = new PortalWorkout("Fuerza", "Pierna", 3, 3,
                List.of(new PortalExercise("Sentadilla", "Piernas", 4, 8, 90)));
        Achievements achievements = new Achievements(member("Ana Gómez"), 12, new Streak(2, 5), List.of(Badge.values()[0]));

        String html = PortalPage.render(summary(member("ana gómez"),
                Optional.of(new PortalMembership("Mensual", LocalDate.of(2026, 12, 14), 33)),
                Optional.of(workout), achievements), "ABCDEFGHJK");

        assertTrue(html.contains("<h1>Ana</h1>"));
        assertTrue(html.contains("Mensual"));
        assertTrue(html.contains("14 de diciembre 2026"));
        assertTrue(html.contains("Pierna"));
        assertTrue(html.contains("día 3 de 3"));
        assertTrue(html.contains("Sentadilla"));
        assertTrue(html.contains("4 × 8"));
        assertTrue(html.contains(Badge.values()[0].getLabel()));
        assertFalse(html.contains("Quedan"));
    }

    @Test
    void warnsWhenTheMembershipEndsInAWeekOrLess() {
        String today = PortalPage.render(summary(member("Ana Gómez"),
                Optional.of(new PortalMembership("Mensual", LocalDate.of(2026, 11, 11), 0)),
                Optional.empty(), nothingYet()), "ABCDEFGHJK");
        String soon = PortalPage.render(summary(member("Ana Gómez"),
                Optional.of(new PortalMembership("Mensual", LocalDate.of(2026, 11, 14), 3)),
                Optional.empty(), nothingYet()), "ABCDEFGHJK");

        assertTrue(today.contains("Vence hoy"));
        assertTrue(soon.contains("Quedan 3 días"));
    }

    @Test
    void explainsWhatIsMissingInsteadOfLeavingEmptySections() {
        String html = PortalPage.render(summary(member("Ana Gómez"), Optional.empty(), Optional.empty(),
                nothingYet()), "ABCDEFGHJK");

        assertTrue(html.contains("No tienes una membresía vigente"));
        assertTrue(html.contains("Todavía no tienes una rutina asignada"));
        assertTrue(html.contains("Todavía no tienes insignias"));
    }

    @Test
    void textFromTheMemberIsEscaped() {
        PortalWorkout workout = new PortalWorkout("<b>Rutina</b>", "Día \"1\"", 1, 1,
                List.of(new PortalExercise("<script>x</script>", "Piernas", 3, 10, 60)));

        String html = PortalPage.render(summary(member("<img src=x> Ana"), Optional.empty(),
                Optional.of(workout), nothingYet()), "ABCDEFGHJK");

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<b>Rutina</b>"));
        assertFalse(html.contains("<img src=x>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void anInvalidCodeGetsAFriendlyPage() {
        assertTrue(PortalPage.invalid().contains("Código no válido"));
    }
}
