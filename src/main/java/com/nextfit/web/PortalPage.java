package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.gamification.Achievements;
import com.nextfit.service.gamification.Badge;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.portal.PortalBestTimes;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.PortalSummary;
import com.nextfit.service.portal.PortalWorkout;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The page a member opens on the phone to see the membership, the routine of the day, the best
 * hours to train, the progress and, when the assistant is on, a chat to ask about his own data. It
 * is a pure function from a summary to text, like {@link AccessPage}, so it is tested without a
 * server. It uses the same colors as the access page so both feel like one product.
 */
public final class PortalPage {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-CO"));
    private static final int SOON_DAYS = 7;

    private PortalPage() {
    }

    /**
     * Draws the portal of a member, without the chat.
     *
     * @param summary what the member sees
     * @param token   the access code, used to ask the server for the member photo
     * @return the HTML page
     */
    public static String render(PortalSummary summary, String token) {
        return render(summary, token, false);
    }

    /**
     * Draws the portal of a member.
     *
     * @param summary     what the member sees
     * @param token       the access code, used to ask the server for the member photo and for the chat
     * @param chatEnabled true when the assistant is on and the chat must be shown
     * @return the HTML page
     */
    public static String render(PortalSummary summary, String token, boolean chatEnabled) {
        StringBuilder body = new StringBuilder();
        header(body, summary.member(), token);
        membership(body, summary);
        workout(body, summary);
        bestTimes(body, summary);
        if (chatEnabled) {
            chat(body, token);
        }
        progress(body, summary.achievements());
        return page(body.toString());
    }

    /**
     * Draws the page of a code that belongs to nobody.
     *
     * @return the HTML page
     */
    public static String invalid() {
        return page("<section class=\"card center\"><h2>Código no válido</h2>"
                + "<p class=\"muted\">Escanea de nuevo el QR de tu carnet o pide uno nuevo en recepción.</p></section>");
    }

    private static void header(StringBuilder body, Member member, String token) {
        body.append("<section class=\"hero\"><div class=\"avatar\"><span>")
                .append(esc(initials(member.getFullName()))).append("</span>");
        if (member.getPhotoPath() != null && !member.getPhotoPath().isBlank()) {
            body.append("<img src=\"/photo/").append(esc(token)).append("\" alt=\"\" onerror=\"this.remove()\">");
        }
        body.append("</div><p class=\"muted\">Hola,</p><h1>").append(esc(firstName(member.getFullName())))
                .append("</h1></section>");
    }

    private static void membership(StringBuilder body, PortalSummary summary) {
        body.append("<section class=\"card\"><h2>Mi membresía</h2>");
        if (summary.membership().isEmpty()) {
            body.append("<p class=\"muted\">No tienes una membresía vigente. Pasa por recepción para renovarla.</p>");
        } else {
            PortalMembership membership = summary.membership().get();
            body.append("<p class=\"big\">").append(esc(membership.planName())).append("</p>")
                    .append("<div class=\"chips\"><span class=\"chip\">Hasta el ")
                    .append(membership.validUntil().format(DAY)).append("</span>");
            if (membership.daysLeft() == 0) {
                body.append("<span class=\"chip warn\">Vence hoy</span>");
            } else if (membership.daysLeft() <= SOON_DAYS) {
                body.append("<span class=\"chip warn\">Quedan ").append(days(membership.daysLeft())).append("</span>");
            }
            body.append("</div>");
        }
        body.append("</section>");
    }

    private static void workout(StringBuilder body, PortalSummary summary) {
        body.append("<section class=\"card\"><h2>Te toca hoy</h2>");
        if (summary.workout().isEmpty()) {
            body.append("<p class=\"muted\">Todavía no tienes una rutina asignada. Pídesela a tu entrenador.</p>");
        } else {
            PortalWorkout workout = summary.workout().get();
            body.append("<p class=\"big\">").append(esc(workout.dayName())).append("</p>")
                    .append("<p class=\"muted\">").append(esc(workout.routineName())).append(" · día ")
                    .append(workout.dayNumber()).append(" de ").append(workout.dayCount()).append("</p>");
            if (workout.exercises().isEmpty()) {
                body.append("<p class=\"muted\">Este día no tiene ejercicios todavía.</p>");
            }
            body.append("<ol class=\"exercises\">");
            for (PortalExercise exercise : workout.exercises()) {
                body.append("<li><b>").append(esc(exercise.name())).append("</b><span>")
                        .append(exercise.sets()).append(" × ").append(exercise.repetitions())
                        .append(" · descanso ").append(exercise.restSeconds()).append(" s · ")
                        .append(esc(exercise.muscleGroup())).append("</span></li>");
            }
            body.append("</ol>");
        }
        body.append("</section>");
    }

    private static void bestTimes(StringBuilder body, PortalSummary summary) {
        body.append("<section class=\"card\"><h2>Mejor hora para entrenar</h2>");
        if (summary.bestTimes().isEmpty()) {
            body.append("<p class=\"muted\">Aún no hay suficientes visitas para recomendarte una hora.</p>");
        } else {
            PortalBestTimes times = summary.bestTimes().get();
            body.append("<p class=\"muted\">").append(esc(times.dayLabel()))
                    .append(" el gimnasio estará más tranquilo a las:</p><div class=\"chips\">");
            for (HourForecast hour : times.hours()) {
                body.append("<span class=\"chip ").append(styleOf(hour.level())).append("\">")
                        .append(String.format("%02d:00", hour.hour())).append(" · ")
                        .append(esc(hour.level().getLabel())).append("</span>");
            }
            body.append("</div>");
        }
        body.append("</section>");
    }

    // EXPLICACIÓN: verde si está tranquilo, ámbar si está muy lleno; los colores viven en el CSS de la página
    private static String styleOf(OccupancyLevel level) {
        return switch (level) {
            case QUIET -> "badge";
            case MODERATE -> "";
            case BUSY, FULL -> "warn";
        };
    }

    // EXPLICACIÓN: el código va en un atributo data-, no dentro del script; y la respuesta se escribe con
    // textContent, así que lo que diga la IA nunca se interpreta como HTML
    private static void chat(StringBuilder body, String token) {
        body.append("<section class=\"card\" id=\"chat\" data-token=\"").append(esc(token)).append("\">")
                .append("<h2>Pregúntale a tu gimnasio</h2>")
                .append("<div id=\"chat-log\" class=\"chat-log\"></div>")
                .append("<form id=\"chat-form\" class=\"chat-form\">")
                .append("<input id=\"chat-input\" type=\"text\" maxlength=\"500\" autocomplete=\"off\" ")
                .append("placeholder=\"¿Qué me toca hoy?\"><button type=\"submit\">Enviar</button></form>")
                .append("<p class=\"muted small\">Solo responde sobre tus propios datos.</p>")
                .append("<script>").append(CHAT_SCRIPT).append("</script></section>");
    }

    private static void progress(StringBuilder body, Achievements achievements) {
        body.append("<section class=\"card\"><h2>Mi progreso</h2><div class=\"stats\">")
                .append(stat(achievements.trainingDays(), "días entrenados"))
                .append(stat(achievements.streak().current(), "semanas de racha"))
                .append(stat(achievements.streak().best(), "mejor racha"))
                .append("</div><h3>Insignias</h3>");
        if (achievements.badges().isEmpty()) {
            body.append("<p class=\"muted\">Todavía no tienes insignias. Entrena hoy y gana la primera.</p>");
        } else {
            body.append("<div class=\"chips\">");
            for (Badge badge : achievements.badges()) {
                body.append("<span class=\"chip badge\" title=\"").append(esc(badge.getDescription())).append("\">")
                        .append(esc(badge.getLabel())).append("</span>");
            }
            body.append("</div>");
        }
        body.append("</section>");
    }

    private static String stat(long value, String label) {
        return "<div class=\"stat\"><b>" + value + "</b><span>" + label + "</span></div>";
    }

    private static final String CHAT_SCRIPT = """
            (function(){
            var box=document.getElementById('chat'),log=document.getElementById('chat-log'),
            form=document.getElementById('chat-form'),input=document.getElementById('chat-input'),
            button=form.querySelector('button');
            function add(text,cls){var p=document.createElement('p');p.className='bubble '+cls;
            p.textContent=text;log.appendChild(p);p.scrollIntoView({block:'nearest'});return p;}
            form.addEventListener('submit',function(e){
            e.preventDefault();var q=input.value.trim();if(!q){return;}
            input.value='';add(q,'me');var wait=add('Pensando…','bot');
            input.disabled=true;button.disabled=true;
            fetch('/m/'+encodeURIComponent(box.dataset.token)+'/chat',{method:'POST',
            headers:{'Content-Type':'application/json'},body:JSON.stringify({question:q})})
            .then(function(r){return r.json().catch(function(){return {error:'No pude responder. Intenta de nuevo'};});})
            .then(function(d){wait.textContent=d.answer||d.error||'No pude responder. Intenta de nuevo';
            if(!d.answer){wait.className='bubble err';}})
            .catch(function(){wait.textContent='No hay conexión con el gimnasio. Intenta de nuevo';
            wait.className='bubble err';})
            .finally(function(){input.disabled=false;button.disabled=false;input.focus();});
            });})();
            """;

    private static final String STYLE = """
            *{box-sizing:border-box}
            body{margin:0;min-height:100vh;padding:22px 16px 34px;color:#fff;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
            background:radial-gradient(120% 60% at 50% -10%,#5A22B8 0%,rgba(90,34,184,0) 60%),#0B0A12}
            .wrap{max-width:440px;margin:0 auto;display:flex;flex-direction:column;gap:16px}
            .brand{display:flex;align-items:center;justify-content:center;gap:10px;margin-bottom:4px}
            .brand img{width:40px;height:40px;border-radius:11px}
            .brand b{font-size:20px;letter-spacing:1px}
            .hero{text-align:center}
            .avatar{position:relative;width:104px;height:104px;margin:0 auto 10px;border-radius:50%;
            background:linear-gradient(135deg,#7A35E8,#5A22B8);display:flex;align-items:center;justify-content:center;
            font-size:38px;font-weight:700;overflow:hidden;box-shadow:0 0 0 4px #0B0A12,0 0 0 7px #A7F626}
            .avatar img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}
            h1{font-size:28px;margin:0}
            h2{font-size:13px;margin:0 0 12px;letter-spacing:2px;text-transform:uppercase;color:#A7F626}
            h3{font-size:14px;margin:18px 0 10px;color:#E4DDF5}
            .card{padding:20px;border-radius:24px;background:linear-gradient(160deg,rgba(255,255,255,.10),rgba(255,255,255,.04));
            border:1px solid rgba(255,255,255,.14);box-shadow:0 18px 44px rgba(0,0,0,.4)}
            .center{text-align:center}
            .big{font-size:22px;font-weight:700;margin:0 0 6px}
            .muted{margin:0;color:#B9AFD6;font-size:15px;line-height:1.4}
            .small{font-size:12px;margin-top:10px}
            .chips{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}
            .chip{padding:8px 14px;border-radius:999px;font-size:14px;background:rgba(122,53,232,.35);border:1px solid rgba(255,255,255,.14)}
            .chip.warn{background:rgba(255,176,32,.18);border-color:rgba(255,176,32,.55);color:#FFD27A;font-weight:600}
            .chip.badge{background:rgba(167,246,38,.14);border-color:rgba(167,246,38,.5);color:#D8FF8A;font-weight:600}
            .exercises{list-style:none;margin:14px 0 0;padding:0;display:flex;flex-direction:column;gap:10px}
            .exercises li{padding:12px 14px;border-radius:16px;background:rgba(255,255,255,.06);display:flex;flex-direction:column;gap:4px}
            .exercises span{font-size:13px;color:#B9AFD6}
            .chat-log{display:flex;flex-direction:column;gap:8px;max-height:320px;overflow-y:auto;margin-bottom:12px}
            .bubble{margin:0;padding:10px 14px;border-radius:16px;font-size:15px;line-height:1.4;max-width:88%;white-space:pre-wrap}
            .bubble.me{align-self:flex-end;background:rgba(122,53,232,.45)}
            .bubble.bot{align-self:flex-start;background:rgba(255,255,255,.08)}
            .bubble.err{align-self:flex-start;background:rgba(255,77,79,.18);color:#FFB3B4}
            .chat-form{display:flex;gap:8px}
            .chat-form input{flex:1;min-width:0;padding:12px 14px;border-radius:14px;border:1px solid rgba(255,255,255,.2);
            background:rgba(255,255,255,.06);color:#fff;font-size:16px}
            .chat-form button{padding:12px 16px;border-radius:14px;border:0;background:#A7F626;color:#0B0A12;font-weight:700;font-size:15px}
            .chat-form button:disabled{opacity:.5}
            .stats{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}
            .stat{padding:14px 6px;border-radius:16px;background:rgba(255,255,255,.06);text-align:center;display:flex;flex-direction:column;gap:4px}
            .stat b{font-size:28px;color:#A7F626}
            .stat span{font-size:12px;color:#B9AFD6}
            footer{margin-top:10px;text-align:center;font-size:12px;letter-spacing:2px;color:#A7F626}
            """;

    private static String page(String body) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"theme-color\" content=\"#0B0A12\">"
                + "<title>Mi portal · NextFit</title><style>" + STYLE + "</style></head><body><div class=\"wrap\">"
                + "<div class=\"brand\"><img src=\"/logo.png\" alt=\"\" onerror=\"this.remove()\"><b>NextFit</b></div>"
                + body + "<footer>ENTRENA. MEJORA. SUPERA TUS LÍMITES.</footer></div></body></html>";
    }

    private static String days(long days) {
        return days == 1 ? "1 día" : days + " días";
    }

    private static String firstName(String fullName) {
        String first = fullName.trim().split("\\s+")[0];
        return first.substring(0, 1).toUpperCase(Locale.ROOT) + first.substring(1).toLowerCase(Locale.ROOT);
    }

    private static String initials(String fullName) {
        StringBuilder result = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && result.length() < 2) {
                result.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return result.toString();
    }

    private static String esc(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}