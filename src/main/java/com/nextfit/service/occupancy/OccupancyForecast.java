package com.nextfit.service.occupancy;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The expected occupation of the gym for every open hour of one day, calculated from the same
 * weekday of the last weeks. It answers "which is the best hour to train?" without any screen
 * knowing how the numbers were made.
 *
 * @param day           the day that is forecast
 * @param hours         one forecast for each open hour, in order
 * @param daysOfHistory how many past days with visits were used; 0 means there is nothing to base it on
 */
public record OccupancyForecast(LocalDate day, List<HourForecast> hours, int daysOfHistory) {

    /** Keeps its own copy of the hours. */
    public OccupancyForecast {
        hours = List.copyOf(hours);
    }

    /**
     * Says whether there was history to calculate from.
     *
     * @return true when at least one past day with visits was used
     */
    public boolean hasHistory() {
        return daysOfHistory > 0;
    }

    /**
     * Returns the quietest hours from a given hour on, so the member is told when to come.
     *
     * @param fromHour the first hour that is useful (for today, the next hour)
     * @param limit    how many hours to return
     * @return the quietest hours, ordered by hour; empty when there is no history
     */
    public List<HourForecast> bestHours(int fromHour, int limit) {
        if (!hasHistory()) {
            return List.of();
        }
        return hours.stream()
                .filter(forecast -> forecast.hour() >= fromHour)
                .sorted(Comparator.comparingDouble(HourForecast::expectedPeople)
                        .thenComparingInt(HourForecast::hour))
                .limit(limit)
                .sorted(Comparator.comparingInt(HourForecast::hour))
                .toList();
    }

    /**
     * Returns the busiest hour of the day.
     *
     * @return the hour with most expected people, or empty when there is no history
     */
    public Optional<HourForecast> busiest() {
        if (!hasHistory()) {
            return Optional.empty();
        }
        return hours.stream().max(Comparator.comparingDouble(HourForecast::expectedPeople)
                .thenComparing(Comparator.comparingInt(HourForecast::hour).reversed()));
    }
}