package com.nextfit.web;

/**
 * The simple pages of the phone server that are not the portal: the connection test and the page of an
 * address that does not exist. They share the NextFit look of the portal. Pure functions from nothing
 * to text, so they are tested without a server.
 */
public final class AccessPage {

    private AccessPage() {
    }

    /**
     * Draws the simple page of the connection test.
     *
     * @return the HTML page
     */
    public static String ping() {
        return page("#2F7DE1", "<div class=\"badge\">" + CHECK + "</div><h1>NextFit responde</h1>"
                + "<p class=\"msg\">Este celular llega al computador. Escanea el QR de tu carnet para ver tu progreso.</p>");
    }

    /**
     * Draws the page of an address that does not exist.
     *
     * @return the HTML page
     */
    public static String notFound() {
        return page("#6B7280", "<div class=\"badge\">" + QUESTION + "</div><h1>Dirección no válida</h1>"
                + "<p class=\"msg\">Escanea el QR de tu carnet de NextFit para ver tu progreso.</p>");
    }

    private static final String SVG_OPEN = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#fff\" stroke-width=\"3\" "
            + "stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">";
    private static final String CHECK = SVG_OPEN + "<path class=\"draw\" d=\"M5 12.5l4.5 4.5L19 7.5\"/></svg>";
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
            .portal{display:inline-block;margin-top:22px;padding:13px 22px;border-radius:999px;background:#A7F626;color:#14101F;
            font-weight:700;font-size:15px;text-decoration:none}
            footer{margin-top:26px;font-size:13px;letter-spacing:2px;color:#A7F626;opacity:0;animation:rise .6s .5s forwards}
            footer span{display:block;margin:0 auto 10px;width:56px;height:4px;border-radius:2px;background:#A7F626}
            @keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
            @keyframes pop{to{transform:scale(1)}}
            @keyframes draw{to{stroke-dashoffset:0}}
            @media (prefers-reduced-motion:reduce){*{animation-duration:.01s!important;animation-delay:0s!important}}
            """;

    static String page(String color, String body) {
        return "<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"theme-color\" content=\"#0B0A12\">"
                + "<title>NextFit</title><style>" + STYLE.formatted(color) + "</style></head><body>"
                + "<div class=\"brand\"><img src=\"/logo.png\" alt=\"\" onerror=\"this.remove()\"><b>NextFit</b></div>"
                + "<main>" + body + "</main>"
                + "<footer><span></span>ENTRENA. MEJORA. SUPERA TUS LÍMITES.</footer></body></html>";
    }
}
