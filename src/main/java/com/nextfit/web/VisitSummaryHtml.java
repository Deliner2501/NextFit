package com.nextfit.web;

import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.PortalWorkout;
import com.nextfit.service.portal.VisitSummary;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Draws the "your day" and "your summary" blocks of the page that follows a QR scan. */
final class VisitSummaryHtml {

    private static final int MAX_EXERCISES = 6;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es"));

    private static final String STYLE = "<style>.vs{margin-top:20px;text-align:left}"
            + ".vs h3{font-size:13px;letter-spacing:2px;margin:0 0 10px;color:#A7F626;text-transform:uppercase}"
            + ".vs .row{padding:12px 14px;margin-top:8px;border-radius:14px;background:rgba(255,255,255,.06);"
            + "border:1px solid rgba(255,255,255,.10)}.vs .row b{display:block;font-size:16px}"
            + ".vs .row span{font-size:13px;color:#BDB3D6}.vs ol{margin:8px 0 0;padding-left:20px;font-size:14px}"
            + ".vs li{margin:4px 0}.vs .stats{display:flex;gap:8px;margin-top:8px}"
            + ".vs .stat{flex:1;text-align:center;padding:10px 6px;border-radius:14px;background:rgba(122,53,232,.30)}"
            + ".vs .stat b{display:block;font-size:20px}.vs .stat span{font-size:12px;color:#E4DDF5}</style>";

    private VisitSummaryHtml() {
    }

    /**
     * @param summary what the member should see
     * @return the HTML block to put inside the result page
     */
    static String render(VisitSummary summary) {
        StringBuilder html = new StringBuilder(STYLE);
        boolean entering = summary.kind() == VisitSummary.Kind.ENTRY;
        html.append("<section class=\"vs\"><h3>").append(entering ? "Tu día" : "Tu resumen").append("</h3>");
        if (!entering) {
            stats(html, summary);
        }
        workout(html, summary, entering);
        membership(html, summary);
        if (entering) {
            bestTimes(html, summary);
        }
        return html.append("</section>").toString();
    }

    private static void stats(StringBuilder html, VisitSummary summary) {
        html.append("<div class=\"stats\">");
        summary.stay().ifPresent(stay -> html.append("<div class=\"stat\"><b>").append(duration(stay))
                .append("</b><span>en el gimnasio</span></div>"));
        html.append("<div class=\"stat\"><b>").append(summary.trainingDays()).append("</b><span>días entrenados</span></div>")
                .append("<div class=\"stat\"><b>").append(summary.streak()).append("</b><span>")
                .append(summary.streak() == 1 ? "semana seguida" : "semanas seguidas").append("</span></div></div>");
    }

    private static void workout(StringBuilder html, VisitSummary summary, boolean entering) {
        String label = entering ? "Te toca hoy" : "Lo que sigue";
        if (summary.workout().isEmpty()) {
            html.append("<div class=\"row\"><b>").append(label).append("</b><span>Todavía no tienes una rutina asignada. "
                    + "Pídesela a tu entrenador.</span></div>");
            return;
        }
        PortalWorkout workout = summary.workout().get();
        html.append("<div class=\"row\"><span>").append(label).append("</span><b>").append(esc(workout.dayName()))
                .append("</b><span>").append(esc(workout.routineName())).append(" · día ").append(workout.dayNumber())
                .append(" de ").append(workout.dayCount()).append("</span>");
        if (entering) {
            List<PortalExercise> exercises = workout.exercises();
            html.append("<ol>");
            for (PortalExercise exercise : exercises.subList(0, Math.min(MAX_EXERCISES, exercises.size()))) {
                html.append("<li>").append(esc(exercise.name())).append(" <span>").append(exercise.sets()).append(" × ")
                        .append(exercise.repetitions()).append("</span></li>");
            }
            html.append("</ol>");
            if (exercises.size() > MAX_EXERCISES) {
                html.append("<span>y ").append(exercises.size() - MAX_EXERCISES).append(" más en tu rutina</span>");
            }
        }
        html.append("</div>");
    }

    private static void membership(StringBuilder html, VisitSummary summary) {
        if (summary.membership().isEmpty()) {
            return;
        }
        PortalMembership membership = summary.membership().get();
        html.append("<div class=\"row\"><span>Tu membresía</span><b>").append(esc(membership.planName()))
                .append("</b><span>Hasta el ").append(membership.validUntil().format(DAY));
        if (membership.daysLeft() == 0) {
            html.append(" · vence hoy");
        } else if (membership.daysLeft() > 0 && membership.daysLeft() <= 7) {
            html.append(" · quedan ").append(membership.daysLeft()).append(membership.daysLeft() == 1 ? " día" : " días");
        }
        html.append("</span></div>");
    }

    private static void bestTimes(StringBuilder html, VisitSummary summary) {
        if (summary.bestTimes().isEmpty()) {
            return;
        }
        var times = summary.bestTimes().get();
        html.append("<div class=\"row\"><span>").append(esc(times.dayLabel()))
                .append(" el gimnasio estará más tranquilo a las</span><b>");
        StringBuilder hours = new StringBuilder();
        for (HourForecast hour : times.hours()) {
            if (hours.length() > 0) {
                hours.append(" · ");
            }
            hours.append(String.format("%02d:00", hour.hour()));
        }
        html.append(hours).append("</b></div>");
    }

    private static String duration(Duration stay) {
        long minutes = stay.toMinutes();
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }

    private static String esc(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
