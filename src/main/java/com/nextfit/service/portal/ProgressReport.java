package com.nextfit.service.portal;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * What the portal shows besides the day to train: the days that come next in the routine, the last
 * visits and how the body measures changed. It is a plain value; it does not know how it is drawn.
 *
 * @param upcoming the days of the routine after the one to train today, in the order they come
 * @param visits   the last visits, newest first
 * @param body     the change of the body measures; empty when the member has fewer than one measure
 */
public record ProgressReport(List<UpcomingDay> upcoming, List<RecentVisit> visits, Optional<BodyProgress> body) {

    public ProgressReport {
        upcoming = List.copyOf(upcoming);
        visits = List.copyOf(visits);
    }

    /**
     * A day of the routine that comes after today.
     *
     * @param dayNumber     the position of the day inside the routine, starting at 1
     * @param name          the name of the day
     * @param exerciseCount how many exercises the day has
     */
    public record UpcomingDay(int dayNumber, String name, int exerciseCount) {
    }

    /**
     * One visit to the gym.
     *
     * @param date  the day
     * @param start the hour of the entry
     * @param stay  how long the member stayed; empty when he has not left yet
     */
    public record RecentVisit(LocalDate date, LocalTime start, Optional<Duration> stay) {
    }

    /**
     * How the weight changed since the first measure.
     *
     * @param since           the day of the first measure
     * @param firstWeightKg   the weight of the first measure
     * @param latestWeightKg  the weight of the last measure
     * @param measures        how many measures the member has
     * @param bmi             the current body mass index, when it can be calculated
     */
    public record BodyProgress(LocalDate since, double firstWeightKg, double latestWeightKg, int measures,
                               Optional<Double> bmi) {
    }
}
