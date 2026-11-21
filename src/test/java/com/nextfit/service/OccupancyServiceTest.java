package com.nextfit.service;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OccupancyServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);   // Wednesday, 10:00 local

    private MemberService members;
    private FakeAttendanceRepository attendanceRepo;
    private AttendanceService attendance;
    private OccupancyService service;
    private Member ana;
    private Member luis;
    private Member carla;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZONE);
        members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        ana = members.register(member("Ana Pérez", "1065123456"));
        luis = members.register(member("Luis Gómez", "1065123457"));
        carla = members.register(member("Carla Ríos", "1065123458"));
        attendanceRepo = new FakeAttendanceRepository();
        attendance = new AttendanceService(members, attendanceRepo, id -> true, clock);
        service = new OccupancyService(attendance, members, 10, clock);
    }

    private static Member member(String name, String document) {
        return new Member(0, name, document, "3001234567", document + "@example.com",
                LocalDate.of(2000, 5, 10), LocalDate.of(2026, 10, 1), "", "Mamá 3110000000", true);
    }

    // Una persona que entró y todavía no ha salido.
    private void open(Member member, LocalDate day, int hour, int minute) {
        attendanceRepo.save(new AttendanceRecord(0, member.getId(),
                LocalDateTime.of(day, LocalTime.of(hour, minute)), null));
    }

    // Una visita ya terminada, de una hora.
    private void visit(Member member, LocalDate day, int hour, int minute) {
        LocalDateTime in = LocalDateTime.of(day, LocalTime.of(hour, minute));
        attendanceRepo.save(new AttendanceRecord(0, member.getId(), in, in.plusHours(1)));
    }

    @Test
    void levelStartsAtItsLimit() {
        assertEquals(OccupancyLevel.QUIET, OccupancyLevel.of(0));
        assertEquals(OccupancyLevel.QUIET, OccupancyLevel.of(39));
        assertEquals(OccupancyLevel.MODERATE, OccupancyLevel.of(40));
        assertEquals(OccupancyLevel.BUSY, OccupancyLevel.of(70));
        assertEquals(OccupancyLevel.FULL, OccupancyLevel.of(90));
        assertEquals(OccupancyLevel.FULL, OccupancyLevel.of(100));
    }

    @Test
    void emptyGymIsQuietAndHasNoUnknownData() {
        OccupancySnapshot snapshot = service.snapshot();

        assertEquals(0, snapshot.inside());
        assertEquals(0, snapshot.percent());
        assertEquals(OccupancyLevel.QUIET, snapshot.level());
        assertTrue(snapshot.visitors().isEmpty());
        assertEquals(24, snapshot.entriesByHour().size());
        assertEquals(-1, snapshot.quietestHour());
    }

    @Test
    void countsOnlyPeopleWhoAreStillInside() {
        OccupancyService small = new OccupancyService(attendance, members, 5, Clock.fixed(
                Instant.parse("2026-11-04T15:00:00Z"), ZONE));
        open(ana, TODAY, 9, 0);
        open(luis, TODAY, 9, 30);
        visit(carla, TODAY, 8, 0);   // ya salió

        OccupancySnapshot snapshot = small.snapshot();

        assertEquals(2, snapshot.inside());
        assertEquals(5, snapshot.capacity());
        assertEquals(40, snapshot.percent());
        assertEquals(OccupancyLevel.MODERATE, snapshot.level());
    }

    @Test
    void visitorsShowTheFirstNameAndTheMinutes() {
        open(luis, TODAY, 9, 30);
        open(ana, TODAY, 9, 0);

        OccupancySnapshot snapshot = service.snapshot();

        assertEquals("Ana", snapshot.visitors().get(0).firstName());
        assertEquals(60, snapshot.visitors().get(0).minutesInside());
        assertEquals("Luis", snapshot.visitors().get(1).firstName());
        assertEquals(30, snapshot.visitors().get(1).minutesInside());
    }

    @Test
    void anEntryOfAnotherDayIsAForgottenExitNotAPersonInside() {
        open(ana, TODAY.minusDays(1), 18, 0);   // entró ayer y nunca salió
        open(luis, TODAY, 3, 0);                // lleva 7 horas pero entró hoy: sigue dentro
        open(carla, TODAY, 9, 0);

        OccupancySnapshot snapshot = service.snapshot();

        assertEquals(2, snapshot.inside());
        assertEquals("Luis", snapshot.visitors().get(0).firstName());
        assertEquals("Carla", snapshot.visitors().get(1).firstName());
    }

    @Test
    void percentIsNeverAboveOneHundred() {
        OccupancyService tiny = new OccupancyService(attendance, members, 2, Clock.fixed(
                Instant.parse("2026-11-04T15:00:00Z"), ZONE));
        open(ana, TODAY, 9, 0);
        open(luis, TODAY, 9, 10);
        open(carla, TODAY, 9, 20);

        OccupancySnapshot snapshot = tiny.snapshot();

        assertEquals(3, snapshot.inside());
        assertEquals(100, snapshot.percent());
        assertEquals(OccupancyLevel.FULL, snapshot.level());
    }

    @Test
    void entriesByHourCountOnlyToday() {
        visit(ana, TODAY, 9, 10);
        visit(luis, TODAY, 9, 40);
        visit(carla, TODAY, 6, 0);
        visit(ana, TODAY.minusDays(1), 9, 0);   // ayer no cuenta

        OccupancySnapshot snapshot = service.snapshot();

        assertEquals(2, snapshot.entriesByHour().get(9).intValue());
        assertEquals(1, snapshot.entriesByHour().get(6).intValue());
        assertEquals(3, snapshot.entriesByHour().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void quietestHourIsTheEmptiestLeftTodayForTheSameWeekday() {
        // Miércoles anteriores: ocupadas las 11, 12 y 13.
        visit(ana, LocalDate.of(2026, 10, 28), 11, 0);
        visit(luis, LocalDate.of(2026, 10, 28), 11, 30);
        visit(carla, LocalDate.of(2026, 10, 21), 12, 0);
        visit(ana, LocalDate.of(2026, 10, 21), 13, 0);
        // El martes está lleno a las 14, pero hoy es miércoles: no debe contar.
        visit(ana, LocalDate.of(2026, 11, 3), 14, 0);
        visit(luis, LocalDate.of(2026, 11, 3), 14, 10);
        visit(carla, LocalDate.of(2026, 11, 3), 14, 20);

        assertEquals(14, service.snapshot().quietestHour());
    }

    @Test
    void quietestHourIsUnknownWithoutHistory() {
        visit(ana, TODAY, 9, 0);   // lo de hoy no es historial

        assertEquals(-1, service.snapshot().quietestHour());
    }

    @Test
    void quietestHourIsUnknownWhenTheGymIsAboutToClose() {
        visit(ana, LocalDate.of(2026, 10, 28), 11, 0);
        OccupancyService late = new OccupancyService(attendance, members, 10, Clock.fixed(
                Instant.parse("2026-11-05T02:30:00Z"), ZONE));   // 21:30 en Bogotá

        assertEquals(-1, late.snapshot().quietestHour());
    }

    @Test
    void capacityMustBeGreaterThanZero() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZONE);

        assertThrows(InvalidDataException.class, () -> new OccupancyService(attendance, members, 0, clock));
    }
}