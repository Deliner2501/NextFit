package com.nextfit.service.churn;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyDropRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    // Days ago of the visits. The last 2 weeks are 0 to 13 days ago; the 4 weeks before are 14 to 41.
    private static final int[] BEFORE_4_PER_WEEK = {14, 16, 18, 20, 21, 23, 25, 27, 28, 30, 32, 34, 35, 37, 39, 41};
    private static final int[] NOW_4_PER_WEEK = {0, 2, 4, 6, 7, 9, 11, 13};

    private final FrequencyDropRule rule = new FrequencyDropRule();

    private static MemberActivity activity(int[] now, int[] before) {
        List<LocalDate> visits = new ArrayList<>();
        for (int daysAgo : now) {
            visits.add(TODAY.minusDays(daysAgo));
        }
        for (int daysAgo : before) {
            visits.add(TODAY.minusDays(daysAgo));
        }
        return new MemberActivity(TODAY, visits, TODAY.minusDays(90), TODAY.plusDays(30), false);
    }

    @Test
    void aSteadyHabitAddsNothing() {
        assertTrue(rule.assess(activity(NOW_4_PER_WEEK, BEFORE_4_PER_WEEK)).isEmpty());
    }

    @Test
    void aBigDropAddsPointsAndExplainsIt() {
        ChurnFactor factor = rule.assess(activity(new int[]{1, 8}, BEFORE_4_PER_WEEK)).orElseThrow();

        assertEquals(30, factor.points());
        assertEquals("Pasó de 4 a 1 visitas por semana", factor.reason());
    }

    @Test
    void stoppingCompletelyAddsTheMaximum() {
        ChurnFactor factor = rule.assess(activity(new int[]{}, BEFORE_4_PER_WEEK)).orElseThrow();

        assertEquals(40, factor.points());
        assertEquals("Pasó de 4 a 0 visitas por semana", factor.reason());
    }

    @Test
    void halfAVisitPerWeekIsWrittenWithAComma() {
        ChurnFactor factor = rule.assess(activity(new int[]{3}, BEFORE_4_PER_WEEK)).orElseThrow();

        assertEquals(35, factor.points());
        assertEquals("Pasó de 4 a 0,5 visitas por semana", factor.reason());
    }

    @Test
    void aSmallDropIsNormalAndAddsNothing() {
        assertTrue(rule.assess(activity(new int[]{0, 2, 4, 6, 8, 10, 12}, BEFORE_4_PER_WEEK)).isEmpty());
    }

    @Test
    void someoneWhoBarelyCameBeforeHasNothingToLose() {
        assertTrue(rule.assess(activity(new int[]{}, new int[]{20, 30, 40})).isEmpty());
    }

    @Test
    void comingMoreThanBeforeAddsNothing() {
        assertTrue(rule.assess(activity(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, BEFORE_4_PER_WEEK)).isEmpty());
    }

    @Test
    void theWeeksMustBeGreaterThanZero() {
        assertThrows(InvalidDataException.class, () -> new FrequencyDropRule(40, 0, 4));
        assertThrows(InvalidDataException.class, () -> new FrequencyDropRule(40, 2, 0));
    }
}