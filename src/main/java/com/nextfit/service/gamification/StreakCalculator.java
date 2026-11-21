package com.nextfit.service.gamification;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calculates the streak of a member from the days he or she trained. A week (Monday to
 * Sunday) counts when the member trained at least a minimum number of different days; the
 * streak is the number of such weeks in a row. The current week, which is still in
 * progress, never breaks the streak: if the goal is not met yet, the count starts from the
 * previous week. The class works on plain dates, so it can be tested without any storage.
 */
public final class StreakCalculator {

    private static final int DAYS_IN_WEEK = 7;

    private StreakCalculator() {
    }

    /**
     * Calculates the current and the best streak.
     *
     * @param visitDays      the days the member trained; repeated days count once and days after today are ignored
     * @param today          the day taken as today
     * @param minDaysPerWeek how many different days a week needs to count, at least 1
     * @return the streak; {@link Streak#NONE} when no week met the goal
     * @throws IllegalArgumentException when {@code minDaysPerWeek} is less than 1
     */
    public static Streak calculate(Collection<LocalDate> visitDays, LocalDate today, int minDaysPerWeek) {
        if (minDaysPerWeek < 1) {
            throw new IllegalArgumentException("Los días mínimos por semana deben ser al menos 1");
        }
        Set<LocalDate> days = new HashSet<>();
        for (LocalDate day : visitDays) {
            if (!day.isAfter(today)) {
                days.add(day);
            }
        }
        Map<LocalDate, Integer> daysPerWeek = new TreeMap<>();
        for (LocalDate day : days) {
            daysPerWeek.merge(weekStart(day), 1, Integer::sum);
        }
        List<LocalDate> goodWeeks = daysPerWeek.entrySet().stream()
                .filter(entry -> entry.getValue() >= minDaysPerWeek)
                .map(Map.Entry::getKey)
                .toList();
        if (goodWeeks.isEmpty()) {
            return Streak.NONE;
        }
        return new Streak(currentRun(goodWeeks, today), longestRun(goodWeeks));
    }

    // The weeks in a row that end this week, or last week when this one has not met the goal yet.
    private static int currentRun(List<LocalDate> goodWeeks, LocalDate today) {
        Set<LocalDate> weeks = new HashSet<>(goodWeeks);
        LocalDate week = weekStart(today);
        if (!weeks.contains(week)) {
            week = week.minusWeeks(1);
        }
        int run = 0;
        while (weeks.contains(week)) {
            run++;
            week = week.minusWeeks(1);
        }
        return run;
    }

    // The goodWeeks come sorted from the oldest to the newest.
    private static int longestRun(List<LocalDate> goodWeeks) {
        int best = 1;
        int run = 1;
        for (int i = 1; i < goodWeeks.size(); i++) {
            long gap = ChronoUnit.DAYS.between(goodWeeks.get(i - 1), goodWeeks.get(i));
            run = gap == DAYS_IN_WEEK ? run + 1 : 1;
            best = Math.max(best, run);
        }
        return best;
    }

    private static LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
