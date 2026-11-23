package com.nextfit.service.portal;

import com.nextfit.service.occupancy.HourForecast;

import java.util.List;
import java.util.Optional;

/**
 * The hours that are best to train, to show them to the member: the quietest ones that are still
 * ahead, today or tomorrow.
 *
 * @param dayLabel "Hoy" or "Mañana", the day the hours belong to
 * @param hours    the quietest hours, in order
 * @param peakHour the busiest estimated hour of that day
 */
public record PortalBestTimes(String dayLabel, List<HourForecast> hours, Optional<HourForecast> peakHour) {

    /**
     * @param dayLabel the day the suggestions belong to
     * @param hours the quietest hours
     */
    public PortalBestTimes(String dayLabel, List<HourForecast> hours) {
        this(dayLabel, hours, Optional.empty());
    }

    /** Keeps its own copy of the hours. */
    public PortalBestTimes {
        hours = List.copyOf(hours);
    }
}