package com.nextfit.service.alert;

import com.nextfit.model.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertMessagesTest {

    private final AlertMessages messages = new AlertMessages("NextFit");

    private Alert alert(AlertType type, String fullName, long days, LocalDate date) {
        Member member = new Member(1, fullName, "10001", "3001234567", "a@b.co",
                LocalDate.of(2000, 1, 1), LocalDate.of(2026, 1, 1), "", "Mamá 3110000000", true);
        return new Alert(type, member, days, date);
    }

    @Test
    void expiringTodayAndTomorrowUseTheirOwnWords() {
        String today = messages.textFor(alert(AlertType.EXPIRING, "ana gómez", 0, LocalDate.of(2026, 11, 8)));
        String tomorrow = messages.textFor(alert(AlertType.EXPIRING, "ana gómez", 1, LocalDate.of(2026, 11, 9)));

        assertTrue(today.contains("vence hoy"));
        assertTrue(tomorrow.contains("vence mañana"));
    }

    @Test
    void expiringInSeveralDaysMentionsTheDateAndTheGym() {
        String text = messages.textFor(alert(AlertType.EXPIRING, "Ana Gómez", 5, LocalDate.of(2026, 11, 13)));

        assertTrue(text.contains("vence en 5 días (13/11/2026)"));
        assertTrue(text.contains("NextFit"));
    }

    @Test
    void firstNameIsCapitalizedWhateverHowItWasTyped() {
        String text = messages.textFor(alert(AlertType.BIRTHDAY, "RONALD mendoza", 21, LocalDate.of(2026, 11, 8)));

        assertTrue(text.startsWith("¡Feliz cumpleaños, Ronald!"));
    }

    @Test
    void inactiveMessageSaysHowManyDays() {
        String text = messages.textFor(alert(AlertType.INACTIVE, "Luis Díaz", 20, LocalDate.of(2026, 10, 19)));

        assertEquals("Hola Luis, hace 20 días que no te vemos en NextFit. "
                + "Tu membresía sigue vigente y tu rutina te espera. ¡Te esperamos!", text);
    }
}
