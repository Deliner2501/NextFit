package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The page a phone shows after scanning a carnet: a full-screen green (entry),
 * blue (exit) or red (denied) answer with the member and the membership. It is a
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
        switch (result.outcome()) {
            case ENTRY -> {
                color = "#1FAE74";
                verdict = "ENTRADA REGISTRADA";
            }
            case EXIT -> {
                color = "#2F7DE1";
                verdict = "SALIDA REGISTRADA";
            }
            default -> {
                color = "#E5484D";
                verdict = "ACCESO DENEGADO";
            }
        }
        StringBuilder body = new StringBuilder();
        Member member = result.member();
        if (member != null) {
            body.append("<div class=\"avatar\">").append(esc(initials(member.getFullName())));
            if (member.getPhotoPath() != null && !member.getPhotoPath().isBlank()) {
                body.append("<img src=\"/photo/").append(esc(token)).append("\" alt=\"\" onerror=\"this.remove()\">");
            }
            body.append("</div>");
        } else {
            body.append("<div class=\"mark\">").append(result.granted() ? "✓" : "✕").append("</div>");
        }
        body.append("<h1>").append(verdict).append("</h1>");
        if (member != null) {
            body.append("<h2>").append(esc(member.getFullName())).append("</h2>");
        }
        body.append("<p>").append(esc(result.message())).append("</p>");
        if (result.granted() && result.validUntil() != null) {
            body.append("<p class=\"small\">Membresía hasta el ").append(result.validUntil().format(DAY));
            if (result.daysLeft() == 0) {
                body.append(" · vence hoy");
            } else if (result.daysLeft() > 0 && result.daysLeft() <= 7) {
                body.append(" · quedan ").append(days(result.daysLeft()));
            }
            body.append("</p>");
        }
        return page(color, body.toString());
    }

    /**
     * Draws the simple page of the connection test.
     *
     * @return the HTML page
     */
    public static String ping() {
        return page("#2F7DE1", "<div class=\"mark\">✓</div><h1>NextFit responde</h1>"
                + "<p>Este celular llega al computador. Escanea el QR de un carnet.</p>");
    }

    /**
     * Draws the page of an address that does not exist.
     *
     * @return the HTML page
     */
    public static String notFound() {
        return page("#6B7280", "<div class=\"mark\">?</div><h1>Dirección no válida</h1>"
                + "<p>Escanea el QR de un carnet de NextFit.</p>");
    }

    private static String page(String color, String body) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>NextFit</title><style>"
                + "body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
                + "background:" + color + ";color:#fff;font-family:system-ui,Segoe UI,Roboto,sans-serif;text-align:center}"
                + "main{padding:32px 24px;max-width:420px}"
                + ".avatar{position:relative;width:140px;height:140px;margin:0 auto 20px;border-radius:30px;"
                + "background:rgba(255,255,255,.22);display:flex;align-items:center;justify-content:center;"
                + "font-size:52px;font-weight:700;overflow:hidden}"
                + ".avatar img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}"
                + ".mark{font-size:96px;line-height:1;margin-bottom:12px}"
                + "h1{font-size:28px;margin:0 0 8px;letter-spacing:.5px}h2{font-size:24px;margin:0 0 16px}"
                + "p{font-size:18px;margin:8px 0}.small{font-size:15px;opacity:.9}"
                + "footer{margin-top:28px;font-size:13px;opacity:.75;letter-spacing:2px}"
                + "</style></head><body><main>" + body + "<footer>NEXTFIT</footer></main></body></html>";
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
