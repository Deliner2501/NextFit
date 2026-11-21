package com.nextfit.service.portal;

import com.nextfit.service.occupancy.HourForecast;

import java.util.List;

/**
 * The hours that are best to train, to show them to the member: the quietest ones that are still
 * ahead, today or tomorrow.
 *
 * @param dayLabel "Hoy" or "Mañana", the day the hours belong to
 * @param hours    the quietest hours, in order
 */
public record PortalBestTimes(String dayLabel, List<HourForecast> hours) {

    /** Keeps its own copy of the hours. */
    public PortalBestTimes {
        hours = List.copyOf(hours);
    }
}