package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.gamification.Achievements;
import com.nextfit.service.gamification.Badge;
import com.nextfit.service.gamification.Streak;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.portal.PortalBestTimes;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.ProgressReport;
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
        return new PortalSummary(member, membership, workout, achievements, Optional.empty());
    }

    private static PortalSummary summaryWithoutData() {
        return summary(member("Ana Gómez"), Optional.empty(), Optional.empty(), nothingYet());
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
        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK");

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

    @Test
    void showsTheBestHoursToTrain() {
        PortalBestTimes times = new PortalBestTimes("Hoy", List.of(
                new HourForecast(17, 0.0, 0, OccupancyLevel.QUIET),
                new HourForecast(19, 40.0, 67, OccupancyLevel.BUSY)), Optional.of(new HourForecast(20, 60.0, 100, OccupancyLevel.FULL)));

        String html = PortalPage.render(new PortalSummary(member("Ana Gómez"), Optional.empty(),
                Optional.empty(), nothingYet(), Optional.of(times)), "ABCDEFGHJK");

        assertTrue(html.contains("Mejor hora para entrenar"));
        assertTrue(html.contains("Hoy · Horas que suelen ser más tranquilas"));
        assertTrue(html.contains("17:00 · Tranquilo"));
        assertTrue(html.contains("19:00 · Concurrido"));
        assertTrue(html.contains("Pico estimado del día · 20:00</strong><br>100 % del aforo previsto"));
    }

    @Test
    void saysSoWhenThereIsNotEnoughHistoryToAdvise() {
        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK");

        assertTrue(html.contains("Aún no hay suficientes visitas"));
    }

    @Test
    void theChatIsShownOnlyWhenTheAssistantIsOn() {
        PortalSummary summary = summaryWithoutData();

        String on = PortalPage.render(summary, "ABCDEFGHJK", true);
        String off = PortalPage.render(summary, "ABCDEFGHJK", false);
        String defaultPage = PortalPage.render(summary, "ABCDEFGHJK");

        assertTrue(on.contains("Pregúntale a tu gimnasio"));
        assertTrue(on.contains("id=\"chat-form\""));
        assertTrue(on.contains("data-token=\"ABCDEFGHJK\""));
        // EXPLICACIÓN: "chat-form" también es una clase del CSS que siempre está en la página; por eso se
        // busca el título de la tarjeta y el id del formulario, que solo existen cuando el chat está encendido
        assertFalse(off.contains("Pregúntale a tu gimnasio"));
        assertFalse(off.contains("id=\"chat-form\""));
        assertFalse(defaultPage.contains("Pregúntale a tu gimnasio"));
        assertFalse(defaultPage.contains("id=\"chat-form\""));
    }

    @Test
    void theCodeInTheChatIsEscaped() {
        String html = PortalPage.render(summaryWithoutData(), "AB\"><script>x</script>", true);

        assertFalse(html.contains("AB\"><script>x</script>"));
        assertTrue(html.contains("data-token=\"AB&quot;&gt;&lt;script&gt;x&lt;/script&gt;\""));
    }

    @Test
    void theAnswerOfTheAiIsWrittenAsTextAndNeverAsHtml() {
        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK", true);

        assertTrue(html.contains("textContent"));
        assertFalse(html.contains("innerHTML"));
    }

    @Test
    void showsTheUpcomingDaysTheLastVisitsAndTheBodyProgress() {
        ProgressReport report = new ProgressReport(
                List.of(new ProgressReport.UpcomingDay(2, "Pecho", 5)),
                List.of(new ProgressReport.RecentVisit(java.time.LocalDate.of(2026, 11, 10),
                        java.time.LocalTime.of(18, 30), Optional.of(java.time.Duration.ofMinutes(75))),
                        new ProgressReport.RecentVisit(java.time.LocalDate.of(2026, 11, 12),
                                java.time.LocalTime.of(7, 5), Optional.empty())),
                Optional.of(new ProgressReport.BodyProgress(java.time.LocalDate.of(2026, 9, 1), 80, 74, 3,
                        Optional.of(24.5))));

        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK", false, Optional.of(report));

        assertTrue(html.contains("Tus próximos días"));
        assertTrue(html.contains("Pecho"));
        assertTrue(html.contains("Tus últimas visitas"));
        assertTrue(html.contains("1 h 15 min"));
        assertTrue(html.contains("Tu cuerpo"));
        assertTrue(html.contains("74"));
    }

    @Test
    void withoutAReportThePortalDoesNotShowTheExtraSections() {
        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK", false, Optional.empty());

        assertFalse(html.contains("Tus próximos días"));
        assertFalse(html.contains("Tus últimas visitas"));
        assertFalse(html.contains("Tu cuerpo"));
    }

    @Test
    void textOfTheDaysIsEscaped() {
        ProgressReport report = new ProgressReport(
                List.of(new ProgressReport.UpcomingDay(2, "<script>x</script>", 1)), List.of(), Optional.empty());

        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK", false, Optional.of(report));

        assertFalse(html.contains("<script>x</script>"));
    }

    @Test
    void theNumberOfTheDayIsNotRepeatedWhenTheNameAlreadyHasIt() {
        ProgressReport report = new ProgressReport(
                List.of(new ProgressReport.UpcomingDay(3, "Día 3 - Piernas", 3)), List.of(), Optional.empty());

        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK", false, Optional.of(report));

        assertTrue(html.contains("<span>3 ejercicios</span>"));
        assertFalse(html.contains("Día 3 · 3"));
    }

    @Test
    void aVisitUnderOneMinuteSaysSo() {
        ProgressReport report = new ProgressReport(List.of(),
                List.of(new ProgressReport.RecentVisit(java.time.LocalDate.of(2026, 11, 14),
                        java.time.LocalTime.of(9, 40), Optional.of(java.time.Duration.ofSeconds(20)))),
                Optional.empty());

        String html = PortalPage.render(summaryWithoutData(), "ABCDEFGHJK", false, Optional.of(report));

        assertTrue(html.contains("menos de 1 min"));
    }
}
