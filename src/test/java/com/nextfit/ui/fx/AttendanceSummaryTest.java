package com.nextfit.ui.fx;

import com.nextfit.model.AttendanceRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttendanceSummaryTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private static AttendanceRecord visit(LocalDateTime in, LocalDateTime out) {
        return new AttendanceRecord(1, 7, in, out);
    }

    @Test
    void countsAllVisitsAndTheOnesOfTheMonth() {
        List<AttendanceRecord> records = List.of(
                visit(LocalDateTime.of(2026, 10, 30, 18, 0), LocalDateTime.of(2026, 10, 30, 19, 0)),
                visit(LocalDateTime.of(2026, 11, 2, 7, 30), LocalDateTime.of(2026, 11, 2, 8, 30)),
                visit(LocalDateTime.of(2026, 11, 6, 18, 15), null));

        AttendanceSummary summary = AttendanceSummary.of(records, NOVEMBER);

        assertEquals(3, summary.total());
        assertEquals(2, summary.thisMonth());
        assertEquals(LocalDateTime.of(2026, 11, 6, 18, 15), summary.last().get());
    }

    @Test
    void findsTheLastVisitWhateverTheOrder() {
        List<AttendanceRecord> records = List.of(
                visit(LocalDateTime.of(2026, 11, 6, 18, 15), null),
                visit(LocalDateTime.of(2026, 11, 2, 7, 30), LocalDateTime.of(2026, 11, 2, 8, 0)));

        assertEquals(LocalDateTime.of(2026, 11, 6, 18, 15), AttendanceSummary.of(records, NOVEMBER).last().get());
    }

    @Test
    void emptyListGivesZerosAndNoLastVisit() {
        AttendanceSummary summary = AttendanceSummary.of(List.of(), NOVEMBER);

        assertEquals(0, summary.total());
        assertEquals(0, summary.thisMonth());
        assertTrue(summary.last().isEmpty());
    }

    @Test
    void durationShowsMinutesOrHoursAndMinutes() {
        LocalDateTime in = LocalDateTime.of(2026, 11, 2, 7, 0);

        assertEquals("45 min", AttendanceSummary.duration(visit(in, in.plusMinutes(45))));
        assertEquals("1 h 20 min", AttendanceSummary.duration(visit(in, in.plusMinutes(80))));
        assertEquals("2 h 0 min", AttendanceSummary.duration(visit(in, in.plusHours(2))));
    }

    @Test
    void durationIsInsideWhileThereIsNoExit() {
        assertEquals("Dentro", AttendanceSummary.duration(visit(LocalDateTime.of(2026, 11, 2, 7, 0), null)));
    }

    @Test
    void durationNeverGoesNegative() {
        LocalDateTime in = LocalDateTime.of(2026, 11, 2, 7, 0);

        assertEquals("0 min", AttendanceSummary.duration(visit(in, in.minusMinutes(5))));
    }
}
