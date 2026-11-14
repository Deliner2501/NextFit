package com.nextfit.service.churn;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaysAwayRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private final DaysAwayRule rule = new DaysAwayRule();

    // A member whose membership started 60 days ago and who visited on the given days before today.
    private static MemberActivity visitedDaysAgo(int... daysAgo) {
        List<LocalDate> visits = java.util.Arrays.stream(daysAgo).mapToObj(TODAY::minusDays).toList();
        return new MemberActivity(TODAY, visits, TODAY.minusDays(60), TODAY.plusDays(30), false);
    }

    @Test
    void aRecentVisitAddsNothing() {
        assertTrue(rule.assess(visitedDaysAgo(2)).isEmpty());
    }

    @Test
    void theFreeDaysAddNothing() {
        assertTrue(rule.assess(visitedDaysAgo(3)).isEmpty());
    }

    @Test
    void thePointsGrowInAStraightLine() {
        Optional<ChurnFactor> factor = rule.assess(visitedDaysAgo(8));

        assertEquals(18, factor.orElseThrow().points());
    }

    @Test
    void theMaximumIsReachedAndNeverPassed() {
        assertEquals(40, rule.assess(visitedDaysAgo(14)).orElseThrow().points());
        assertEquals(40, rule.assess(visitedDaysAgo(40)).orElseThrow().points());
    }

    @Test
    void theReasonSaysHowManyDays() {
        assertEquals("Lleva 10 días sin venir", rule.assess(visitedDaysAgo(10)).orElseThrow().reason());
    }

    @Test
    void withoutVisitsTheDaysAreCountedFromTheStartOfTheMembership() {
        MemberActivity noVisits = new MemberActivity(TODAY, List.of(), TODAY.minusDays(10), TODAY.plusDays(20), false);

        assertEquals(25, rule.assess(noVisits).orElseThrow().points());
    }

    @Test
    void aVisitBeforeTheMembershipStartedDoesNotCount() {
        // He visited 30 days ago but renewed 2 days ago: he is not absent, he has just come back.
        MemberActivity renewed = new MemberActivity(TODAY, List.of(TODAY.minusDays(30)),
                TODAY.minusDays(2), TODAY.plusDays(28), false);

        assertTrue(rule.assess(renewed).isEmpty());
    }

    @Test
    void theMaximumDaysMustBeMoreThanTheFreeDays() {
        assertThrows(InvalidDataException.class, () -> new DaysAwayRule(40, 5, 5));
    }
}