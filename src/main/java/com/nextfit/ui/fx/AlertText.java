package com.nextfit.ui.fx;

import com.nextfit.service.alert.Alert;
import com.nextfit.ui.Formats;

/**
 * Writes the detail line that the alert center shows for each alert, for example
 * "Vence en 3 días · 14 nov 2026". It is a plain class without JavaFX, so the wording can
 * be tested without a window. The message that goes to the member is not written here:
 * that one comes from {@code AlertMessages}.
 */
final class AlertText {

    private AlertText() {
    }

    /**
     * Writes the detail of an alert for the staff.
     *
     * @param alert the alert
     * @return a short text with the number and the date that go with the alert
     */
    static String detail(Alert alert) {
        String date = Formats.date(alert.date());
        long days = alert.days();
        return switch (alert.type()) {
            case EXPIRING -> days == 0 ? "Vence hoy"
                    : days == 1 ? "Vence mañana"
                    : "Vence en " + days + " días · " + date;
            case INACTIVE -> days + (days == 1 ? " día" : " días") + " sin venir · desde " + date;
            case BIRTHDAY -> "Cumple " + days + (days == 1 ? " año" : " años") + " · " + date;
        };
    }
}
