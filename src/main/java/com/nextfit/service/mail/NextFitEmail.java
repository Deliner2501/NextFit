package com.nextfit.service.mail;

import java.util.List;

/** Builds matching HTML and plain-text versions of a NextFit notification. */
record NextFitEmail(String subject, String text, String html) {

    /** A labeled value shown in the summary card. */
    record Detail(String label, String value) {}

    /**
     * Creates a branded email with escaped member data and a plain-text alternative.
     *
     * @param subject the inbox subject
     * @param title the heading displayed inside the email
     * @param memberName the recipient's first and last name
     * @param introduction a short description of the notification
     * @param details the values to show in the summary card
     * @param closing a final instruction or note
     * @return both versions of the same notification
     */
    static NextFitEmail create(String subject, String title, String memberName, String introduction,
                               List<Detail> details, String closing) {
        String greeting = "Hola, " + memberName + ".";
        StringBuilder text = new StringBuilder("NextFit\n\n").append(title).append("\n\n")
                .append(greeting).append("\n\n").append(introduction).append("\n\n");
        StringBuilder rows = new StringBuilder();
        for (Detail detail : details) {
            text.append(detail.label()).append(": ").append(detail.value()).append('\n');
            rows.append("""
                    <tr><td style="padding:10px 16px;color:#d9c9f3;font-size:15px;overflow-wrap:anywhere;">%s</td>
                    <td style="padding:10px 16px;color:#d4ff6a;font-size:15px;font-weight:bold;text-align:right;overflow-wrap:anywhere;">%s</td></tr>
                    """.formatted(escape(detail.label()), escape(detail.value())));
        }
        text.append("\n").append(closing).append("\n\nEquipo NextFit");
        String html = """
                <!doctype html>
                <html lang="es"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title></head>
                <body style="margin:0;background:#eee8f8;color:#f8f4ff;font-family:Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#eee8f8;">
                <tr><td align="center" style="padding:20px 12px;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                    style="max-width:560px;background:#1b112f;border-radius:16px;border:1px solid #6746a6;">
                <tr><td style="padding:22px 24px;background:#2d194d;border-bottom:3px solid #a7f626;border-radius:16px 16px 0 0;">
                <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                <td style="padding-right:12px;"><img src="cid:nextfit-logo" alt="" width="40" height="40"
                    style="display:block;border:0;border-radius:10px;"></td>
                <td><strong style="font-size:25px;color:#fff;">Next<span style="color:#baff49;">Fit</span></strong></td>
                </tr></table>
                </td></tr>
                <tr><td style="padding:24px;color:#f8f4ff;font-size:16px;line-height:1.55;">
                <h1 style="font-size:24px;line-height:1.25;margin:0 0 18px;">%s</h1>
                <p style="margin:0 0 8px;">%s</p><p style="margin:0 0 18px;">%s</p>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                    style="background:#332052;border:1px solid #6746a6;border-radius:10px;table-layout:fixed;">%s</table>
                <p style="margin:18px 0 0;">%s</p>
                </td></tr>
                <tr><td style="padding:0 24px 22px;color:#d9c9f3;font-size:13px;">Equipo NextFit</td></tr>
                </table></td></tr></table></body></html>
                """.formatted(escape(title), escape(title), escape(greeting), escape(introduction), rows, escape(closing));
        return new NextFitEmail(subject, text.toString(), html);
    }

    /** Prefixes the test subject without changing either message body. */
    NextFitEmail asTest() {
        return new NextFitEmail("[PRUEBA] " + subject, text, html);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
