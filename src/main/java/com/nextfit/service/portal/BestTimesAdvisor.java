package com.nextfit.service.portal;

import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyForecastService;
import com.nextfit.service.occupancy.OccupancyService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Tells a member which hours are best to train. It recommends the quietest hours that are still
 * ahead today and, when today has no hours left (or no history), the quietest of tomorrow. When
 * there is no history at all it recommends nothing instead of inventing something.
 */
public class BestTimesAdvisor {

    /** How many hours are recommended. */
    public static final int HOURS_TO_RECOMMEND = 3;

    private final OccupancyForecastService forecast;
    private final Clock clock;

    /**
     * Creates the advisor.
     *
     * @param forecast the forecast of the occupation by hour
     * @param clock    the source of the current date and time
     */
    public BestTimesAdvisor(OccupancyForecastService forecast, Clock clock) {
        this.forecast = forecast;
        this.clock = clock;
    }

    /**
     * Finds the best hours to train from now on.
     *
     * @return the recommended hours, or empty when there is not enough history
     */
    public Optional<PortalBestTimes> advise() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<HourForecast> today = forecast.forecastFor(now.toLocalDate())
                .bestHours(now.getHour() + 1, HOURS_TO_RECOMMEND);
        if (!today.isEmpty()) {
            return Optional.of(new PortalBestTimes("Hoy", today));
        }
        List<HourForecast> tomorrow = forecast.forecastFor(now.toLocalDate().plusDays(1))
                .bestHours(OccupancyService.OPEN_HOUR, HOURS_TO_RECOMMEND);
        return tomorrow.isEmpty() ? Optional.empty() : Optional.of(new PortalBestTimes("Mañana", tomorrow));
    }
}