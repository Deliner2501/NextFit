package com.nextfit.ui.fx;

import com.nextfit.model.AttendanceRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * The numbers shown at the top of the attendance tab of a member: how many times the
 * member came, how many this month and when was the last visit.
 *
 * @param total     all the visits
 * @param thisMonth the visits of the given month
 * @param last      the moment of the most recent entry, empty when there are none
 */
record AttendanceSummary(int total, long thisMonth, Optional<LocalDateTime> last) {

    /**
     * Summarizes the visits of one member.
     *
     * @param records the visits of the member, in any order
     * @param month   the month to count apart
     * @return the summary
     */
    static AttendanceSummary of(List<AttendanceRecord> records, YearMonth month) {
        long inMonth = records.stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .count();
        Optional<LocalDateTime> last = records.stream()
                .map(AttendanceRecord::getCheckIn)
                .max(LocalDateTime::compareTo);
        return new AttendanceSummary(records.size(), inMonth, last);
    }

    /**
     * Writes how long a visit lasted.
     *
     * @param record the visit
     * @return "Dentro" while the member has not left, otherwise e.g. "1 h 20 min" or "45 min"
     */
    static String duration(AttendanceRecord record) {
        Optional<LocalDateTime> exit = record.getCheckOut();
        if (exit.isEmpty()) {
            return "Dentro";
        }
        long minutes = Math.max(0, Duration.between(record.getCheckIn(), exit.get()).toMinutes());
        long hours = minutes / 60;
        long rest = minutes % 60;
        return hours == 0 ? rest + " min" : hours + " h " + rest + " min";
    }
}
