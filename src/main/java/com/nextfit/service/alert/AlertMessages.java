package com.nextfit.service.alert;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Writes the friendly message the gym sends to the member for each alert. It
 * only builds the text: it does not know WhatsApp, screens or phone numbers.
 */
public class AlertMessages {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private final String gymName;

    /**
     * Creates the writer.
     *
     * @param gymName the name of the gym that signs the messages
     */
    public AlertMessages(String gymName) {
        this.gymName = gymName;
    }

    /**
     * Writes the message for an alert.
     *
     * @param alert the alert
     * @return the text to send to the member
     */
    public String textFor(Alert alert) {
        String name = firstName(alert.member().getFullName());
        return switch (alert.type()) {
            case EXPIRING -> expiringText(name, alert);
            case INACTIVE -> "Hola " + name + ", hace " + alert.days() + " días que no te vemos en " + gymName
                    + ". Tu membresía sigue vigente y tu rutina te espera. ¡Te esperamos!";
            case BIRTHDAY -> "¡Feliz cumpleaños, " + name + "! Todo el equipo de " + gymName
                    + " te desea un gran día. Pasa a entrenar y celebra con nosotros.";
        };
    }

    private String expiringText(String name, Alert alert) {
        String when;
        if (alert.days() == 0) {
            when = "vence hoy";
        } else if (alert.days() == 1) {
            when = "vence mañana";
        } else {
            when = "vence en " + alert.days() + " días (" + alert.date().format(DATE) + ")";
        }
        return "Hola " + name + ", tu membresía en " + gymName + " " + when
                + ". ¿Quieres renovarla para seguir entrenando sin pausas?";
    }

    private static String firstName(String fullName) {
        String first = fullName.trim().split("\\s+")[0];
        return first.substring(0, 1).toUpperCase(Locale.ROOT) + first.substring(1).toLowerCase(Locale.ROOT);
    }
}
