package com.nextfit.service.portal;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.persistence.CsvAttendanceRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyForecastService;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the advice of the best hours; today is Friday 13 November 2026. */
class BestTimesAdvisorTest {

    private static final Clock BASE = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate LAST_FRIDAY = LocalDate.of(2026, 11, 6);
    private static final LocalDate LAST_SATURDAY = LocalDate.of(2026, 11, 7);

    private CsvAttendanceRepository attendanceRepository;
    private AttendanceService attendance;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        attendanceRepository = new CsvAttendanceRepository(folder.resolve("attendance.csv"));
        MemberService members = new MemberService(
                new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        SubscriptionService subscriptions = new SubscriptionService(
                new CsvSubscriptionRepository(folder.resolve("subscriptions.csv")), plans, members, BASE);
        attendance = new AttendanceService(members, attendanceRepository, subscriptions, BASE);
    }

    private BestTimesAdvisor advisorAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new BestTimesAdvisor(new OccupancyForecastService(attendance, clock), clock);
    }

    private void visit(LocalDate day, int inHour, int outHour, int people) {
        for (int i = 0; i < people; i++) {
            attendanceRepository.save(new AttendanceRecord(0, 100 + i, day.atTime(inHour, 0), day.atTime(outHour, 0)));
        }
    }

    private static List<Integer> hoursOf(PortalBestTimes times) {
        return times.hours().stream().map(HourForecast::hour).toList();
    }

    @Test
    void recommendsTheQuietestHoursThatAreStillAheadToday() {
        visit(LAST_FRIDAY, 16, 17, 3);
        visit(LAST_FRIDAY, 18, 19, 2);

        PortalBestTimes times = advisorAt("2026-11-13T15:00:00Z").advise().orElseThrow();

        assertEquals("Hoy", times.dayLabel());
        assertEquals(List.of(17, 19, 20), hoursOf(times));
        assertEquals(16, times.peakHour().orElseThrow().hour());
    }

    @Test
    void neverRecommendsTheHourThatIsRunningOrOneThatPassed() {
        visit(LAST_FRIDAY, 18, 19, 2);

        PortalBestTimes times = advisorAt("2026-11-13T17:30:00Z").advise().orElseThrow();

        assertTrue(hoursOf(times).stream().allMatch(hour -> hour >= 18));
        assertEquals(18, times.peakHour().orElseThrow().hour());
    }

    @Test
    void lateAtNightItRecommendsTheHoursOfTomorrow() {
        visit(LAST_FRIDAY, 18, 19, 2);
        visit(LAST_SATURDAY, 6, 7, 2);

        PortalBestTimes times = advisorAt("2026-11-13T21:30:00Z").advise().orElseThrow();

        assertEquals("Mañana", times.dayLabel());
        assertEquals(List.of(5, 7, 8), hoursOf(times));
    }

    @Test
    void ifTodayHasNoHistoryButTomorrowHasItUsesTomorrow() {
        visit(LAST_SATURDAY, 6, 7, 2);

        PortalBestTimes times = advisorAt("2026-11-13T15:00:00Z").advise().orElseThrow();

        assertEquals("Mañana", times.dayLabel());
    }

    @Test
    void withoutHistoryItRecommendsNothing() {
        Optional<PortalBestTimes> times = advisorAt("2026-11-13T15:00:00Z").advise();

        assertTrue(times.isEmpty());
    }
}