package com.nextfit.service.occupancy;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.persistence.CsvAttendanceRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvMembershipPlanRepository;
import com.nextfit.persistence.CsvSubscriptionRepository;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.service.SubscriptionService;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the occupancy forecast with real CSV files in a temporary folder and a fixed clock:
 * today is Friday 13 November 2026, so the history of a Friday is 6 Nov, 30, 23, 16, 9 and 2 Oct, 25 and 18 Sep.
 */
class OccupancyForecastServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 13);

    private CsvAttendanceRepository attendanceRepository;
    private AttendanceService attendance;
    private OccupancyForecastService service;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        attendanceRepository = new CsvAttendanceRepository(folder.resolve("attendance.csv"));
        MemberService members = new MemberService(
                new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        SubscriptionService subscriptions = new SubscriptionService(
                new CsvSubscriptionRepository(folder.resolve("subscriptions.csv")), plans, members, CLOCK);
        attendance = new AttendanceService(members, attendanceRepository, subscriptions, CLOCK);
        service = new OccupancyForecastService(attendance, CLOCK);
    }

    private void visit(LocalDate day, int inHour, int inMinute, int outHour, int outMinute, int people) {
        for (int i = 0; i < people; i++) {
            attendanceRepository.save(new AttendanceRecord(0, 100 + i,
                    day.atTime(inHour, inMinute), day.atTime(outHour, outMinute)));
        }
    }

    private static HourForecast at(OccupancyForecast forecast, int hour) {
        return forecast.hours().stream().filter(h -> h.hour() == hour).findFirst().orElseThrow();
    }

    @Test
    void withoutHistoryThereIsNothingToForecast() {
        OccupancyForecast forecast = service.forecastFor(FRIDAY);

        assertFalse(forecast.hasHistory());
        assertEquals(0, forecast.daysOfHistory());
        assertTrue(forecast.bestHours(5, 3).isEmpty());
        assertTrue(forecast.busiest().isEmpty());
    }

    @Test
    void hasOneForecastForEveryOpenHour() {
        OccupancyForecast forecast = service.forecastFor(FRIDAY);

        assertEquals(OccupancyService.CLOSE_HOUR - OccupancyService.OPEN_HOUR, forecast.hours().size());
        assertEquals(OccupancyService.OPEN_HOUR, forecast.hours().get(0).hour());
        assertEquals(OccupancyService.CLOSE_HOUR - 1, forecast.hours().get(forecast.hours().size() - 1).hour());
    }

    @Test
    void averagesTheSameWeekdayOfThePastWeeks() {
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 3);
        visit(LocalDate.of(2026, 10, 30), 18, 0, 19, 0, 1);

        OccupancyForecast forecast = service.forecastFor(FRIDAY);

        assertEquals(2, forecast.daysOfHistory());
        assertEquals(2.0, at(forecast, 18).expectedPeople());
        assertEquals(3, at(forecast, 18).percent());
        assertEquals(0.0, at(forecast, 17).expectedPeople());
    }

    @Test
    void otherWeekdaysDoNotCount() {
        visit(LocalDate.of(2026, 11, 5), 18, 0, 19, 0, 5);   // a Thursday

        assertFalse(service.forecastFor(FRIDAY).hasHistory());
    }

    @Test
    void aPersonIsInsideEveryHourBetweenTheEntryAndTheExit() {
        visit(LocalDate.of(2026, 11, 6), 17, 30, 19, 15, 1);

        OccupancyForecast forecast = service.forecastFor(FRIDAY);

        assertEquals(1.0, at(forecast, 17).expectedPeople());
        assertEquals(1.0, at(forecast, 18).expectedPeople());
        assertEquals(1.0, at(forecast, 19).expectedPeople());
        assertEquals(0.0, at(forecast, 20).expectedPeople());
    }

    @Test
    void anExitAtTheExactHourDoesNotCountTheNextHour() {
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 1);

        OccupancyForecast forecast = service.forecastFor(FRIDAY);

        assertEquals(1.0, at(forecast, 18).expectedPeople());
        assertEquals(0.0, at(forecast, 19).expectedPeople());
    }

    @Test
    void aForgottenExitCountsAsOneHour() {
        attendanceRepository.save(new AttendanceRecord(0, 100, LocalDate.of(2026, 11, 6).atTime(18, 10), null));

        OccupancyForecast forecast = service.forecastFor(FRIDAY);

        assertEquals(1.0, at(forecast, 18).expectedPeople());
        assertEquals(1.0, at(forecast, 19).expectedPeople());
        assertEquals(0.0, at(forecast, 20).expectedPeople());
    }

    @Test
    void theBestHoursAreTheQuietestOnesFromAGivenHour() {
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 3);
        visit(LocalDate.of(2026, 11, 6), 6, 0, 7, 0, 2);

        List<HourForecast> best = service.forecastFor(FRIDAY).bestHours(17, 2);

        assertEquals(List.of(17, 19), best.stream().map(HourForecast::hour).toList());
    }

    @Test
    void theBusiestHourIsTheOneWithMostPeople() {
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 3);
        visit(LocalDate.of(2026, 11, 6), 6, 0, 7, 0, 2);

        HourForecast busiest = service.forecastFor(FRIDAY).busiest().orElseThrow();

        assertEquals(18, busiest.hour());
        assertEquals(3.0, busiest.expectedPeople());
    }

    @Test
    void aFutureDayOnlyUsesPastDaysAndNeverToday() {
        visit(LocalDate.of(2026, 11, 13), 18, 0, 19, 0, 9);   // today: not history yet
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 1);

        OccupancyForecast forecast = service.forecastFor(LocalDate.of(2026, 11, 20));

        assertEquals(1, forecast.daysOfHistory());
        assertEquals(1.0, at(forecast, 18).expectedPeople());
    }

    @Test
    void theLevelFollowsThePercentOfTheCapacity() {
        service = new OccupancyForecastService(attendance, 10, CLOCK);
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 9);

        HourForecast hour = at(service.forecastFor(FRIDAY), 18);

        assertEquals(90, hour.percent());
        assertEquals(OccupancyLevel.FULL, hour.level());
    }

    @Test
    void thePercentNeverPassesOneHundred() {
        service = new OccupancyForecastService(attendance, 10, CLOCK);
        visit(LocalDate.of(2026, 11, 6), 18, 0, 19, 0, 15);

        assertEquals(100, at(service.forecastFor(FRIDAY), 18).percent());
    }
}