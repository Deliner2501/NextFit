package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.occupancy.OccupancyService;
import com.nextfit.service.occupancy.OccupancySnapshot;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the forecast of the day that the TV screen shows inside the occupancy snapshot. */
class OccupancyForecastOnTvTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate LAST_WEDNESDAY = LocalDate.of(2026, 10, 28);

    private FakeAttendanceRepository attendanceRepo;
    private OccupancyService service;
    private Member ana;

    @BeforeEach
    void setUp() {
        // Wednesday 4 Nov 2026, 10:00 local time.
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZONE);
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        ana = members.register(new Member(0, "Ana Pérez", "1065123456", "3001234567",
                "ana@example.com", LocalDate.of(2000, 5, 10), LocalDate.of(2026, 10, 1), "",
                "Mamá 3110000000", true));
        attendanceRepo = new FakeAttendanceRepository();
        AttendanceService attendance = new AttendanceService(members, attendanceRepo, id -> true, clock);
        service = new OccupancyService(attendance, members, 10, clock);
    }

    private void visit(LocalDate day, int hour) {
        LocalDateTime in = LocalDateTime.of(day, LocalTime.of(hour, 0));
        attendanceRepo.save(new AttendanceRecord(0, ana.getId(), in, in.plusHours(1)));
    }

    @Test
    void thereIsNoForecastWithoutHistory() {
        assertTrue(service.snapshot().forecast().isEmpty());
    }

    @Test
    void todayDoesNotCountAsHistory() {
        visit(LocalDate.of(2026, 11, 4), 9);

        assertTrue(service.snapshot().forecast().isEmpty());
    }

    @Test
    void theForecastHasOneEntryForEveryOpenHour() {
        visit(LAST_WEDNESDAY, 11);

        List<HourForecast> forecast = service.snapshot().forecast();

        assertEquals(OccupancyService.CLOSE_HOUR - OccupancyService.OPEN_HOUR, forecast.size());
        assertEquals(OccupancyService.OPEN_HOUR, forecast.get(0).hour());
        assertEquals(OccupancyService.CLOSE_HOUR - 1, forecast.get(forecast.size() - 1).hour());
    }

    @Test
    void theForecastUsesTheSameWeekdayAndTheCapacity() {
        visit(LAST_WEDNESDAY, 11);   // 1 person of 10 → 10 %

        HourForecast eleven = service.snapshot().forecast().stream()
                .filter(h -> h.hour() == 11).findFirst().orElseThrow();

        assertEquals(10, eleven.percent());
        assertEquals(OccupancyLevel.QUIET, eleven.level());
    }

    @Test
    void theBestHourIsOneOfTheQuietestHoursOfTheForecast() {
        visit(LAST_WEDNESDAY, 11);
        OccupancySnapshot snapshot = service.snapshot();

        int best = snapshot.quietestHour();
        HourForecast chosen = snapshot.forecast().stream()
                .filter(h -> h.hour() == best).findFirst().orElseThrow();

        assertTrue(best > 10);
        assertEquals(0, chosen.percent());
    }
}