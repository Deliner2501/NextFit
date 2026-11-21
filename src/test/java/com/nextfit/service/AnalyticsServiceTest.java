package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Subscription;
import com.nextfit.service.BillingFakeRepositories.FakePlanRepository;
import com.nextfit.service.BillingFakeRepositories.FakeSubscriptionRepository;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4); // Wednesday

    private FakeAttendanceRepository attendanceRepo;
    private FakeSubscriptionRepository subscriptionRepo;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZONE);
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(new FakePlanRepository(), new MembershipPlanValidator());
        subscriptionRepo = new FakeSubscriptionRepository();
        SubscriptionService subscriptions = new SubscriptionService(subscriptionRepo, plans, members, clock);
        attendanceRepo = new FakeAttendanceRepository();
        AttendanceService attendance = new AttendanceService(members, attendanceRepo, subscriptions, clock);
        service = new AnalyticsService(attendance, subscriptions, clock);
    }

    private void visit(long member, LocalDateTime in, LocalDateTime out) {
        attendanceRepo.save(new AttendanceRecord(0, member, in, out));
    }

    private void subscription(long member, LocalDate start, LocalDate end) {
        subscriptionRepo.save(new Subscription(0, member, 1, start, end, false));
    }

    @Test
    void heatmapCountsEntriesByWeekdayAndHour() {
        visit(1, LocalDateTime.of(2026, 11, 4, 18, 10), null);   // Wednesday 18h
        visit(2, LocalDateTime.of(2026, 10, 28, 18, 45), null);  // Wednesday 18h
        visit(3, LocalDateTime.of(2026, 11, 2, 6, 0), null);     // Monday 6h

        OccupancyHeatmap map = service.report().heatmap();

        assertEquals(2, map.count(DayOfWeek.WEDNESDAY, 18));
        assertEquals(1, map.count(DayOfWeek.MONDAY, 6));
        assertEquals(3, map.total());
        assertEquals(DayOfWeek.WEDNESDAY, map.peak().day());
        assertEquals(18, map.peak().hour());
        assertEquals(2, map.max());
    }

    @Test
    void heatmapIgnoresEntriesOlderThanTheWindow() {
        visit(1, LocalDateTime.of(2026, 8, 1, 10, 0), null);

        assertEquals(0, service.report().heatmap().total());
        assertEquals(0, service.report().heatmap().peak().count());
    }

    @Test
    void averageStayUsesOnlyClosedVisits() {
        visit(1, LocalDateTime.of(2026, 11, 3, 10, 0), LocalDateTime.of(2026, 11, 3, 11, 0));
        visit(2, LocalDateTime.of(2026, 11, 3, 10, 0), LocalDateTime.of(2026, 11, 3, 10, 30));
        visit(3, LocalDateTime.of(2026, 11, 4, 10, 0), null);

        assertEquals(45, service.report().averageStayMin());
    }

    @Test
    void averageStayIgnoresForgottenExits() {
        visit(1, LocalDateTime.of(2026, 11, 3, 10, 0), LocalDateTime.of(2026, 11, 3, 11, 0));
        visit(2, LocalDateTime.of(2026, 11, 3, 8, 0), LocalDateTime.of(2026, 11, 3, 23, 0)); // 15 hours

        assertEquals(60, service.report().averageStayMin());
    }

    @Test
    void averageStayIsZeroWithoutClosedVisits() {
        assertEquals(0, service.report().averageStayMin());
    }

    @Test
    void renewalRateComparesEndedMembershipsWithLaterOnes() {
        // Member 1 ended 10 days ago and renewed; member 2 ended 20 days ago and did not.
        subscription(1, TODAY.minusDays(40), TODAY.minusDays(10));
        subscription(1, TODAY.minusDays(9), TODAY.plusDays(20));
        subscription(2, TODAY.minusDays(50), TODAY.minusDays(20));

        AnalyticsService.Report report = service.report();

        assertEquals(2, report.renewalBase());
        assertEquals(0.5, report.renewalRate(), 0.0001);
    }

    @Test
    void renewalRateIsMinusOneWhenNothingEnded() {
        subscription(1, TODAY.minusDays(5), TODAY.plusDays(25));

        assertEquals(-1.0, service.report().renewalRate(), 0.0001);
    }

    @Test
    void atRiskListsActiveMembersWhoStoppedComing() {
        subscription(1, TODAY.minusDays(30), TODAY.plusDays(10)); // came 20 days ago -> at risk
        subscription(2, TODAY.minusDays(30), TODAY.plusDays(10)); // came yesterday -> fine
        subscription(3, TODAY.minusDays(30), TODAY.plusDays(10)); // never came -> at risk, first
        subscription(4, TODAY.minusDays(3), TODAY.plusDays(27));  // joined 3 days ago -> too new
        visit(1, TODAY.minusDays(20).atTime(9, 0), null);
        visit(2, TODAY.minusDays(1).atTime(9, 0), null);

        List<AnalyticsService.AtRisk> atRisk = service.report().atRisk();

        assertEquals(2, atRisk.size());
        assertEquals(3, atRisk.get(0).memberId());
        assertTrue(atRisk.get(0).lastVisit() == null);
        assertEquals(1, atRisk.get(1).memberId());
        assertEquals(TODAY.minusDays(20), atRisk.get(1).lastVisit());
    }
}
