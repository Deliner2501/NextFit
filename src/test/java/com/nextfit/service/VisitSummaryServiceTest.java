package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult.Outcome;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.service.gamification.Achievements;
import com.nextfit.service.gamification.Streak;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.occupancy.OccupancyLevel;
import com.nextfit.service.portal.PortalBestTimes;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalMembership;
import com.nextfit.service.portal.PortalSummary;
import com.nextfit.service.portal.PortalWorkout;
import com.nextfit.service.portal.VisitSummary;
import com.nextfit.service.portal.VisitSummaryService;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitSummaryServiceTest {

    private static final class MovingClock extends Clock {
        private Instant now = Instant.parse("2026-11-14T15:00:00Z");

        void advanceMinutes(long minutes) {
            now = now.plusSeconds(minutes * 60);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MovingClock clock;
    private AttendanceService attendance;
    private Member member;
    private VisitSummaryService service;
    private Optional<PortalSummary> portal;

    @BeforeEach
    void setUp() {
        clock = new MovingClock();
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        member = members.register(FakeRepositories.newMember("1065123456"));
        attendance = new AttendanceService(members, new FakeAttendanceRepository(), id -> true, clock);
        PortalWorkout workout = new PortalWorkout("Fuerza", "Pierna", 2, 3,
                List.of(new PortalExercise("Sentadilla", "Piernas", 4, 10, 60)));
        PortalMembership membership = new PortalMembership("Mensual", LocalDate.of(2026, 11, 30), 16);
        PortalBestTimes times = new PortalBestTimes("Hoy", List.of(new HourForecast(10, 4.0, 20, OccupancyLevel.QUIET)));
        portal = Optional.of(new PortalSummary(member, Optional.of(membership), Optional.of(workout),
                new Achievements(member, 12, new Streak(3, 5), List.of()), Optional.of(times)));
        service = new VisitSummaryService(m -> portal, attendance);
    }

    private AccessResult result(Outcome outcome, Member who) {
        return new AccessResult(outcome, who, "ok", null, -1);
    }

    @Test
    void entryShowsTheDayOfTheRoutineTheMembershipAndTheBestHours() {
        VisitSummary summary = service.of(result(Outcome.ENTRY, member)).orElseThrow();

        assertEquals(VisitSummary.Kind.ENTRY, summary.kind());
        assertEquals("Pierna", summary.workout().orElseThrow().dayName());
        assertEquals(16, summary.membership().orElseThrow().daysLeft());
        assertTrue(summary.bestTimes().isPresent());
        assertTrue(summary.stay().isEmpty());
    }

    @Test
    void exitShowsHowLongTheMemberStayedAndTheProgress() {
        attendance.checkIn(member.getId());
        clock.advanceMinutes(75);
        attendance.checkOut(member.getId());

        VisitSummary summary = service.of(result(Outcome.EXIT, member)).orElseThrow();

        assertEquals(VisitSummary.Kind.EXIT, summary.kind());
        assertEquals(Duration.ofMinutes(75), summary.stay().orElseThrow());
        assertEquals(12, summary.trainingDays());
        assertEquals(3, summary.streak());
        assertTrue(summary.bestTimes().isEmpty());
    }

    @Test
    void exitWithoutAClosedVisitHasNoStay() {
        assertTrue(service.of(result(Outcome.EXIT, member)).orElseThrow().stay().isEmpty());
    }

    @Test
    void aDeniedAccessOrAnUnknownMemberHasNoSummary() {
        assertTrue(service.of(result(Outcome.DENIED, member)).isEmpty());
        assertTrue(service.of(result(Outcome.ENTRY, null)).isEmpty());
        portal = Optional.empty();
        assertTrue(service.of(result(Outcome.ENTRY, member)).isEmpty());
    }
}
