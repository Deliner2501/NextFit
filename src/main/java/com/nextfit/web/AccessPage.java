package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The page a phone shows after scanning a carnet: a NextFit branded card with a green (entry),
 * blue (exit) or red (denied) status, the member photo and the membership. It is a
 * pure function from a result to text, so it is tested without a server.
 */
public final class AccessPage {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-CO"));

    private AccessPage() {
    }

    /**
     * Draws the answer of a scan.
     *
     * @param result the result of the access
     * @param token  the scanned code, used to ask the server for the member photo
     * @return the HTML page
     */
    public static String render(AccessResult result, String token) {
        String color;
        String verdict;
        String icon;
        switch (result.outcome()) {
            case ENTRY -> {
                color = "#1FAE74";
                verdict = "ENTRADA REGISTRADA";
                icon = CHECK;
            }
            case EXIT -> {
                color = "#2F7DE1";
                verdict = "SALIDA REGISTRADA";
                icon = EXIT_ICON;
            }
            default -> {
                color = "#E5484D";
                verdict = "ACCESO DENEGADO";
                icon = CROSS;
            }
        }
        StringBuilder body = new StringBuilder();
        body.append("<div class=\"badge\">").append(icon).append("</div>");
        Member member = result.member();
        if (member != null) {
            body.append("<div class=\"avatar\"><span>").append(esc(initials(member.getFullName()))).append("</span>");
            if (member.getPhotoPath() != null && !member.getPhotoPath().isBlank()) {
                body.append("<img src=\"/photo/").append(esc(token)).append("\" alt=\"\" onerror=\"this.remove()\">");
            }
            body.append("</div>");
            body.append("<h2>").append(esc(member.getFullName())).append("</h2>");
        }
        body.append("<h1>").append(verdict).append("</h1>");
        body.append("<p class=\"msg\">").append(esc(result.message())).append("</p>");
        if (result.granted() && result.validUntil() != null) {
            body.append("<div class=\"chips\"><span class=\"chip\">Hasta el ")
                    .append(result.validUntil().format(DAY)).append("</span>");
            if (result.daysLeft() == 0) {
                body.append("<span class=\"chip warn\">Vence hoy</span>");
            } else if (result.daysLeft() > 0 && result.daysLeft() <= 7) {
                body.append("<span class=\"chip warn\">Quedan ").append(days(result.daysLeft())).append("</span>");
            }
            body.append("</div>");
        }
        return page(color, body.toString());
    }

    /**
     * Draws the simple page of the connection test.
     *
     * @return the HTML page
     */
    public static String ping() {
        return page("#2F7DE1", "<div class=\"badge\">" + CHECK + "</div><h1>NextFit responde</h1>"
                + "<p class=\"msg\">Este celular llega al computador. Escanea el QR de un carnet.</p>");
    }

    /**
     * Draws the page of an address that does not exist.
     *
     * @return the HTML page
     */
    public static String notFound() {
        return page("#6B7280", "<div class=\"badge\">" + QUESTION + "</div><h1>Dirección no válida</h1>"
                + "<p class=\"msg\">Escanea el QR de un carnet de NextFit.</p>");
    }

    private static final String SVG_OPEN = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#fff\" stroke-width=\"3\" "
            + "stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">";
    private static final String CHECK = SVG_OPEN + "<path class=\"draw\" d=\"M5 12.5l4.5 4.5L19 7.5\"/></svg>";
    private static final String CROSS = SVG_OPEN + "<path class=\"draw\" d=\"M6 6l12 12\"/><path class=\"draw\" d=\"M18 6L6 18\"/></svg>";
    private static final String EXIT_ICON = SVG_OPEN + "<path class=\"draw\" d=\"M10 5H5v14h5\"/>"
            + "<path class=\"draw\" d=\"M16 8l4 4-4 4\"/><path class=\"draw\" d=\"M20 12H9\"/></svg>";
    private static final String QUESTION = SVG_OPEN + "<path class=\"draw\" d=\"M9.5 9a2.5 2.5 0 1 1 3.6 2.2c-.7.4-1.1 1-1.1 1.8\"/>"
            + "<path d=\"M12 17.5h.01\"/></svg>";

    private static final String STYLE = """
            :root{--accent:%s}
            *{box-sizing:border-box}
            body{margin:0;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;
            padding:24px 18px;color:#fff;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;text-align:center;
            background:radial-gradient(120%% 70%% at 50%% -10%%,#5A22B8 0%%,rgba(90,34,184,0) 60%%),
            radial-gradient(90%% 60%% at 50%% 115%%,color-mix(in srgb,var(--accent) 45%%,transparent) 0%%,rgba(0,0,0,0) 70%%),#0B0A12}
            .brand{display:flex;align-items:center;gap:10px;margin-bottom:64px;opacity:0;animation:rise .6s .05s forwards}
            .brand img{width:44px;height:44px;border-radius:12px}
            .brand b{font-size:22px;letter-spacing:1px}
            main{width:100%%;max-width:400px;padding:34px 24px 30px;border-radius:30px;
            background:linear-gradient(160deg,rgba(255,255,255,.10),rgba(255,255,255,.04));
            border:1px solid rgba(255,255,255,.14);box-shadow:0 24px 60px rgba(0,0,0,.45),0 0 70px color-mix(in srgb,var(--accent) 28%%,transparent);
            opacity:0;animation:rise .6s .15s forwards}
            .badge{width:84px;height:84px;margin:-76px auto 18px;border-radius:50%%;background:var(--accent);
            display:flex;align-items:center;justify-content:center;border:5px solid #0B0A12;
            box-shadow:0 0 0 2px var(--accent),0 10px 30px color-mix(in srgb,var(--accent) 55%%,transparent);
            transform:scale(0);animation:pop .55s .35s cubic-bezier(.2,1.6,.4,1) forwards}
            .badge svg{width:42px;height:42px}
            .draw{stroke-dasharray:40;stroke-dashoffset:40;animation:draw .5s .7s ease-out forwards}
            .avatar{position:relative;width:132px;height:132px;margin:6px auto 18px;border-radius:50%%;
            background:linear-gradient(135deg,#7A35E8,#5A22B8);display:flex;align-items:center;justify-content:center;
            font-size:46px;font-weight:700;overflow:hidden;box-shadow:0 0 0 4px #0B0A12,0 0 0 7px var(--accent)}
            .avatar img{position:absolute;inset:0;width:100%%;height:100%%;object-fit:cover}
            h2{font-size:25px;margin:0 0 6px;line-height:1.2}
            h1{font-size:15px;margin:0 0 14px;letter-spacing:2.5px;color:var(--accent);filter:brightness(1.25)}
            .msg{font-size:18px;margin:8px 0 4px;color:#E4DDF5}
            .chips{display:flex;flex-wrap:wrap;gap:8px;justify-content:center;margin-top:18px}
            .chip{padding:8px 14px;border-radius:999px;font-size:14px;background:rgba(122,53,232,.35);border:1px solid rgba(255,255,255,.14)}
            .chip.warn{background:rgba(255,176,32,.18);border-color:rgba(255,176,32,.55);color:#FFD27A;font-weight:600}
            footer{margin-top:26px;font-size:13px;letter-spacing:2px;color:#A7F626;opacity:0;animation:rise .6s .5s forwards}
            footer span{display:block;margin:0 auto 10px;width:56px;height:4px;border-radius:2px;background:#A7F626}
            @keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
            @keyframes pop{to{transform:scale(1)}}
            @keyframes draw{to{stroke-dashoffset:0}}
            @media (prefers-reduced-motion:reduce){*{animation-duration:.01s!important;animation-delay:0s!important}}
            """;

    private static String page(String color, String body) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"theme-color\" content=\"#0B0A12\">"
                + "<title>NextFit</title><style>" + STYLE.formatted(color) + "</style></head><body>"
                + "<div class=\"brand\"><img src=\"/logo.png\" alt=\"\" onerror=\"this.remove()\"><b>NextFit</b></div>"
                + "<main>" + body + "</main>"
                + "<footer><span></span>ENTRENA. MEJORA. SUPERA TUS LÍMITES.</footer></body></html>";
    }

    private static String days(long days) {
        return days == 1 ? "1 día" : days + " días";
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
