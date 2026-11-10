package com.nextfit.service.gamification;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreakCalculatorTest {

    // A Sunday: the current week goes from Monday 2 to Sunday 8 of November.
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final LocalDate THIS_MONDAY = LocalDate.of(2026, 11, 2);
    private static final int GOAL = 3;

    // The first {@code days} days (Monday, Tuesday...) of the week that started {@code weeksAgo} weeks before this one.
    private static List<LocalDate> week(int weeksAgo, int days) {
        List<LocalDate> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            result.add(THIS_MONDAY.minusWeeks(weeksAgo).plusDays(i));
        }
        return result;
    }

    private static Streak streak(List<LocalDate> days) {
        return StreakCalculator.calculate(days, TODAY, GOAL);
    }

    @Test
    void noVisitsGiveNoStreak() {
        assertEquals(Streak.NONE, streak(List.of()));
    }

    @Test
    void aWeekBelowTheGoalDoesNotCount() {
        assertEquals(new Streak(0, 0), streak(week(0, 2)));
    }

    @Test
    void aWeekThatMeetsTheGoalIsAStreakOfOne() {
        assertEquals(new Streak(1, 1), streak(week(0, 3)));
    }

    @Test
    void theWeekInProgressDoesNotBreakTheStreak() {
        List<LocalDate> days = new ArrayList<>(week(1, 3));
        days.addAll(week(0, 1));

        assertEquals(new Streak(1, 1), streak(days));
    }

    @Test
    void weeksInARowAreAdded() {
        List<LocalDate> days = new ArrayList<>();
        for (int weeksAgo = 0; weeksAgo <= 3; weeksAgo++) {
            days.addAll(week(weeksAgo, 3));
        }

        assertEquals(new Streak(4, 4), streak(days));
    }

    @Test
    void aMissedWeekBreaksTheCurrentStreakButNotTheBest() {
        List<LocalDate> days = new ArrayList<>();
        days.addAll(week(0, 3));
        days.addAll(week(2, 3));
        days.addAll(week(3, 3));

        assertEquals(new Streak(1, 2), streak(days));
    }

    @Test
    void theBestStreakCanBeOlderThanTheCurrentOne() {
        List<LocalDate> days = new ArrayList<>();
        for (int weeksAgo = 6; weeksAgo <= 10; weeksAgo++) {
            days.addAll(week(weeksAgo, 4));
        }
        days.addAll(week(0, 3));

        assertEquals(new Streak(1, 5), streak(days));
    }

    @Test
    void whenTheLastWeeksAreMissedTheCurrentStreakIsZero() {
        List<LocalDate> days = new ArrayList<>(week(5, 3));
        days.addAll(week(6, 3));

        assertEquals(new Streak(0, 2), streak(days));
    }

    @Test
    void repeatedDaysCountOnce() {
        List<LocalDate> days = new ArrayList<>();
        days.add(THIS_MONDAY);
        days.add(THIS_MONDAY);
        days.add(THIS_MONDAY);
        days.add(THIS_MONDAY.plusDays(1));

        assertEquals(Streak.NONE, streak(days));
    }

    @Test
    void daysAfterTodayAreIgnored() {
        List<LocalDate> days = new ArrayList<>(week(0, 2));
        days.add(TODAY.plusDays(1));
        days.add(TODAY.plusDays(2));

        assertEquals(Streak.NONE, streak(days));
    }

    @Test
    void theWeekStartsOnMonday() {
        // Friday to Sunday belong to one week and Monday to Wednesday to the next one.
        List<LocalDate> days = List.of(
                LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 4));

        assertEquals(new Streak(2, 2), StreakCalculator.calculate(days, LocalDate.of(2026, 11, 4), GOAL));
    }

    @Test
    void aGoalOfOneDayCountsEveryWeekWithAVisit() {
        List<LocalDate> days = List.of(THIS_MONDAY, THIS_MONDAY.minusWeeks(1), THIS_MONDAY.minusWeeks(2));

        assertEquals(new Streak(3, 3), StreakCalculator.calculate(days, TODAY, 1));
    }

    @Test
    void theGoalMustBeAtLeastOneDay() {
        assertThrows(IllegalArgumentException.class, () -> StreakCalculator.calculate(List.of(), TODAY, 0));
    }
}
