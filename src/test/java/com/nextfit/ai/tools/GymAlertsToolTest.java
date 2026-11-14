package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Member;
import com.nextfit.service.alert.Alert;
import com.nextfit.service.alert.AlertRule;
import com.nextfit.service.alert.AlertService;
import com.nextfit.service.alert.AlertType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the tool "gym alerts" with fixed alerts; today is 13 November 2026. */
class GymAlertsToolTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private static final AiUser ADMIN = new AiUser(AiAudience.ADMIN, 0, "admin");

    private static Member member(long id, String name) {
        return new Member(id, name, "1000" + id, "3001234567", "socio@example.com",
                LocalDate.of(1998, 3, 1), LocalDate.of(2026, 1, 5), "", "Mamá 3110000000", true);
    }

    private static GymAlertsTool toolWith(Alert... alerts) {
        AlertRule rule = today -> new ArrayList<>(List.of(alerts));
        return new GymAlertsTool(new AlertService(List.of(rule), CLOCK));
    }

    private static Alert expiring(long id, String name, long days) {
        return new Alert(AlertType.EXPIRING, member(id, name), days, LocalDate.of(2026, 11, 16));
    }

    private static Alert inactive(long id, String name, long days) {
        return new Alert(AlertType.INACTIVE, member(id, name), days, LocalDate.of(2026, 10, 30));
    }

    private static Alert birthday(long id, String name, long age) {
        return new Alert(AlertType.BIRTHDAY, member(id, name), age, LocalDate.of(2026, 11, 13));
    }

    @Test
    void showsTheThreeTypesWithTheirMeaning() {
        String text = toolWith(expiring(1, "Ana Gómez", 3), inactive(2, "Luis Pérez", 20),
                birthday(3, "Carla Ruiz", 28)).run(Map.of(), ADMIN);

        assertTrue(text.contains("Por vencer (1):"));
        assertTrue(text.contains("Ana Gómez: su membresía vence en 3 día(s) (16/11/2026)"));
        assertTrue(text.contains("Luis Pérez: lleva 20 días sin venir"));
        assertTrue(text.contains("Carla Ruiz: cumple 28 años hoy"));
    }

    @Test
    void anExpiryTodayIsSaidAsToday() {
        String text = toolWith(expiring(1, "Ana Gómez", 0)).run(Map.of(), ADMIN);

        assertTrue(text.contains("vence hoy"));
    }

    @Test
    void canAskForOneTypeOnly() {
        GymAlertsTool tool = toolWith(expiring(1, "Ana Gómez", 3), inactive(2, "Luis Pérez", 20));

        String text = tool.run(Map.of("type", "inactivos"), ADMIN);

        assertTrue(text.contains("Luis Pérez"));
        assertFalse(text.contains("Ana Gómez"));
    }

    @Test
    void theLimitIsAppliedPerType() {
        GymAlertsTool tool = toolWith(inactive(1, "Ana Gómez", 15), inactive(2, "Luis Pérez", 20),
                inactive(3, "Carla Ruiz", 30));

        String text = tool.run(Map.of("limit", 2L), ADMIN);

        assertTrue(text.contains("Inactivo (3):"));
        assertTrue(text.contains("se muestran los 2 primeros"));
        assertEquals(2, text.lines().filter(line -> line.startsWith("- ")).count());
    }

    @Test
    void saysSoWhenThereAreNoAlerts() {
        assertEquals("No hay alertas de ese tipo por ahora.", toolWith().run(Map.of(), ADMIN));
    }

    @Test
    void doesNotGivePhonesOrDocuments() {
        String text = toolWith(expiring(1, "Ana Gómez", 3)).run(Map.of(), ADMIN);

        assertFalse(text.contains("3001234567"));
        assertFalse(text.contains("10001"));
        assertFalse(text.contains("socio@example.com"));
    }

    @Test
    void aWrongTypeIsRejectedWithAClearMessage() {
        assertThrows(InvalidDataException.class, () -> toolWith().run(Map.of("type", "todas"), ADMIN));
    }

    @Test
    void onlyAdminAndReceptionMayUseIt() {
        GymAlertsTool tool = toolWith();

        assertTrue(tool.allowedFor(AiAudience.ADMIN));
        assertTrue(tool.allowedFor(AiAudience.RECEPTION));
        assertFalse(tool.allowedFor(AiAudience.TRAINER));
        assertFalse(tool.allowedFor(AiAudience.MEMBER));
    }
}