package com.nextfit.service.occupancy;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.service.AttendanceService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Forecasts how full the gym will be at each hour of a day. The rule is simple and can be
 * explained: it takes the same weekday of the last {@value #HISTORY_WEEKS} weeks, counts how many
 * people were inside at each hour (from their entry to their exit) and averages those days.
 * Days without any visit are not counted, so a holiday does not lower the average.
 */
public class OccupancyForecastService {

    /** How many past weeks are used. */
    public static final int HISTORY_WEEKS = 8;

    private final AttendanceService attendance;
    private final int capacity;
    private final Clock clock;

    /**
     * Creates the service with the usual capacity of the gym.
     *
     * @param attendance the entries and exits
     * @param clock      the source of today's date
     */
    public OccupancyForecastService(AttendanceService attendance, Clock clock) {
        this(attendance, OccupancyService.DEFAULT_CAPACITY, clock);
    }

    /**
     * Creates the service.
     *
     * @param attendance the entries and exits
     * @param capacity   how many people fit in the gym at the same time
     * @param clock      the source of today's date
     */
    public OccupancyForecastService(AttendanceService attendance, int capacity, Clock clock) {
        this.attendance = attendance;
        this.capacity = capacity;
        this.clock = clock;
    }

    /**
     * Forecasts a day, which may be today or a future day.
     *
     * @param day the day to forecast
     * @return the expected occupation for each open hour
     */
    public OccupancyForecast forecastFor(LocalDate day) {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, int[]> presence = new HashMap<>();
        for (int week = 1; week <= HISTORY_WEEKS; week++) {
            LocalDate date = day.minusWeeks(week);
            // EXPLICACIÓN: para un día futuro, una de las "semanas anteriores" puede ser hoy o futura; no sirve
            if (date.isBefore(today)) {
                presence.put(date, new int[24]);
            }
        }
        for (AttendanceRecord record : attendance.findAll()) {
            int[] hours = presence.get(record.getCheckIn().toLocalDate());
            if (hours != null) {
                markPresence(record, hours);
            }
        }
        List<int[]> daysWithVisits = presence.values().stream()
                .filter(hours -> IntStream.of(hours).sum() > 0)
                .toList();
        List<HourForecast> forecasts = new ArrayList<>();
        for (int hour = OccupancyService.OPEN_HOUR; hour < OccupancyService.CLOSE_HOUR; hour++) {
            double expected = averageAt(daysWithVisits, hour);
            int percent = (int) Math.min(100, Math.round(expected * 100.0 / capacity));
            forecasts.add(new HourForecast(hour, expected, percent, OccupancyLevel.of(percent)));
        }
        return new OccupancyForecast(day, forecasts, daysWithVisits.size());
    }

    private static double averageAt(List<int[]> days, int hour) {
        if (days.isEmpty()) {
            return 0;
        }
        return days.stream().mapToInt(hours -> hours[hour]).sum() / (double) days.size();
    }

    // EXPLICACIÓN: la persona cuenta en cada hora entre su entrada y su salida. Una salida olvidada
    // (registro abierto de un día pasado) se supone de una hora, para no inflar todas las horas.
    private static void markPresence(AttendanceRecord record, int[] hours) {
        LocalDateTime start = record.getCheckIn();
        LocalDateTime end = record.getCheckOut().orElse(start.plusHours(1));
        int first = start.getHour();
        int last;
        if (end.toLocalDate().isAfter(start.toLocalDate())) {
            last = 23;
        } else if (end.getMinute() == 0 && end.getHour() > first) {
            last = end.getHour() - 1;
        } else {
            last = Math.max(first, end.getHour());
        }
        for (int hour = first; hour <= last; hour++) {
            hours[hour]++;
        }
    }
}