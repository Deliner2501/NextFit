package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.alert.Alert;
import com.nextfit.service.alert.AlertType;
import com.nextfit.ui.Formats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertTextTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 14);

    private static Alert alert(AlertType type, long days) {
        Member member = new Member(1, "Ana Gómez", "10001", "3001234567", "a@b.co",
                LocalDate.of(2000, 1, 1), LocalDate.of(2026, 1, 1), "", "Mamá 3110000000", true);
        return new Alert(type, member, days, DAY);
    }

    @Test
    void expiringTodayAndTomorrowUseTheirOwnWords() {
        assertEquals("Vence hoy", AlertText.detail(alert(AlertType.EXPIRING, 0)));
        assertEquals("Vence mañana", AlertText.detail(alert(AlertType.EXPIRING, 1)));
    }

    @Test
    void expiringInSeveralDaysShowsTheDaysAndTheLastDay() {
        assertEquals("Vence en 3 días · " + Formats.date(DAY), AlertText.detail(alert(AlertType.EXPIRING, 3)));
    }

    @Test
    void inactiveShowsHowManyDaysAndSince() {
        assertEquals("14 días sin venir · desde " + Formats.date(DAY), AlertText.detail(alert(AlertType.INACTIVE, 14)));
    }

    @Test
    void inactiveSingularDoesNotSayDays() {
        assertEquals("1 día sin venir · desde " + Formats.date(DAY), AlertText.detail(alert(AlertType.INACTIVE, 1)));
    }

    @Test
    void birthdayShowsTheAgeTheMemberTurns() {
        assertEquals("Cumple 26 años · " + Formats.date(DAY), AlertText.detail(alert(AlertType.BIRTHDAY, 26)));
    }
}
