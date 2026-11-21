package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;

import java.time.DayOfWeek;
import java.util.Collection;

/**
 * Counts entries by day of the week and hour, to see when the gym is busy.
 * It is a pure calculation over records: no storage, no clock, no screen.
 */
public final class OccupancyHeatmap {

    /** One cell of the heatmap. */
    public record Slot(DayOfWeek day, int hour, long count) {
    }

    private final long[][] counts = new long[7][24];

    private OccupancyHeatmap() {
    }

    /**
     * Builds the heatmap from the entries of the given records.
     *
     * @param records the attendance records to count
     * @return the heatmap
     */
    public static OccupancyHeatmap of(Collection<AttendanceRecord> records) {
        OccupancyHeatmap map = new OccupancyHeatmap();
        for (AttendanceRecord r : records) {
            map.counts[r.getCheckIn().getDayOfWeek().getValue() - 1][r.getCheckIn().getHour()]++;
        }
        return map;
    }

    /**
     * Returns the entries of one cell.
     *
     * @param day  the day of the week
     * @param hour the hour of the day, 0 to 23
     * @return the number of entries
     */
    public long count(DayOfWeek day, int hour) {
        return counts[day.getValue() - 1][hour];
    }

    /**
     * Returns the highest value of any cell, to scale the colors.
     *
     * @return the maximum, 0 when there are no entries
     */
    public long max() {
        long max = 0;
        for (long[] row : counts) {
            for (long value : row) {
                max = Math.max(max, value);
            }
        }
        return max;
    }

    /**
     * Returns the total entries counted.
     *
     * @return the total
     */
    public long total() {
        long total = 0;
        for (long[] row : counts) {
            for (long value : row) {
                total += value;
            }
        }
        return total;
    }

    /**
     * Returns the busiest cell. When several tie, the earliest in the week wins.
     *
     * @return the peak slot, or a slot with count 0 when there are no entries
     */
    public Slot peak() {
        Slot best = new Slot(DayOfWeek.MONDAY, 0, 0);
        for (DayOfWeek day : DayOfWeek.values()) {
            for (int hour = 0; hour < 24; hour++) {
                long value = count(day, hour);
                if (value > best.count()) {
                    best = new Slot(day, hour, value);
                }
            }
        }
        return best;
    }
}
