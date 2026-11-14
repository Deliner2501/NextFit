package com.nextfit.service.churn;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpiringSoonRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private final ExpiringSoonRule rule = new ExpiringSoonRule();

    private static MemberActivity endingIn(int daysLeft, boolean renewed) {
        return new MemberActivity(TODAY, List.of(), TODAY.minusDays(25), TODAY.plusDays(daysLeft), renewed);
    }

    @Test
    void aMembershipThatEndsLaterAddsNothing() {
        assertTrue(rule.assess(endingIn(7, false)).isEmpty());
        assertTrue(rule.assess(endingIn(20, false)).isEmpty());
    }

    @Test
    void onTheLastDayItAddsTheMaximum() {
        ChurnFactor factor = rule.assess(endingIn(0, false)).orElseThrow();

        assertEquals(20, factor.points());
        assertEquals("Su membresía vence hoy y no ha renovado", factor.reason());
    }

    @Test
    void tomorrowIsWrittenAsTomorrow() {
        ChurnFactor factor = rule.assess(endingIn(1, false)).orElseThrow();

        assertEquals(17, factor.points());
        assertEquals("Su membresía vence mañana y no ha renovado", factor.reason());
    }

    @Test
    void theFewerDaysAreLeftTheMorePoints() {
        int threeDays = rule.assess(endingIn(3, false)).orElseThrow().points();
        int sixDays = rule.assess(endingIn(6, false)).orElseThrow().points();

        assertEquals(11, threeDays);
        assertEquals(3, sixDays);
        assertEquals("Su membresía vence en 3 días y no ha renovado",
                rule.assess(endingIn(3, false)).orElseThrow().reason());
    }

    @Test
    void aRenewedMembershipAddsNothing() {
        assertTrue(rule.assess(endingIn(0, true)).isEmpty());
    }

    @Test
    void anAlreadyEndedMembershipAddsNothing() {
        assertTrue(rule.assess(endingIn(-2, false)).isEmpty());
    }

    @Test
    void theDaysMustBeGreaterThanZero() {
        assertThrows(InvalidDataException.class, () -> new ExpiringSoonRule(20, 0));
    }
}