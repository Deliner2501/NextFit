package com.nextfit.service.alert;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.Subscription;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the alerts with real CSV files in a temporary folder and a fixed
 * clock: today is 8 November 2026.
 */
class AlertServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    private CsvMemberRepository memberRepository;
    private CsvSubscriptionRepository subscriptionRepository;
    private CsvAttendanceRepository attendanceRepository;
    private MemberService members;
    private SubscriptionService subscriptions;
    private AttendanceService attendance;
    private AlertService service;
    private int nextDocument = 10000;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        memberRepository = new CsvMemberRepository(folder.resolve("members.csv"));
        subscriptionRepository = new CsvSubscriptionRepository(folder.resolve("subscriptions.csv"));
        attendanceRepository = new CsvAttendanceRepository(folder.resolve("attendance.csv"));
        members = new MemberService(memberRepository, new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        subscriptions = new SubscriptionService(subscriptionRepository, plans, members, CLOCK);
        attendance = new AttendanceService(members, attendanceRepository, subscriptions, CLOCK);
        service = AlertService.withStandardRules(subscriptions, attendance, members, CLOCK);
    }

    private Member member(String name, LocalDate birthDate, boolean active) {
        return memberRepository.save(new Member(0, name, String.valueOf(nextDocument++), "3001234567",
                "socio@example.com", birthDate, LocalDate.of(2026, 1, 5), "", "Mamá 3110000000", active));
    }

    private Member member(String name) {
        return member(name, LocalDate.of(1998, 3, 1), true);
    }

    private void subscribe(Member member, LocalDate start, LocalDate end) {
        subscriptionRepository.save(new Subscription(0, member.getId(), 1, start, end, false));
    }

    private void visit(Member member, LocalDate day) {
        attendanceRepository.save(new AttendanceRecord(0, member.getId(), LocalDateTime.of(day, java.time.LocalTime.of(18, 0)),
                LocalDateTime.of(day, java.time.LocalTime.of(19, 0))));
    }

    // ----- memberships about to end -----

    @Test
    void membershipEndingInThreeDaysIsReported() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(27), TODAY.plusDays(3));
        visit(ana, TODAY.minusDays(1));

        List<Alert> alerts = service.findByType(AlertType.EXPIRING);

        assertEquals(1, alerts.size());
        assertEquals(ana.getId(), alerts.get(0).member().getId());
        assertEquals(3, alerts.get(0).days());
        assertEquals(TODAY.plusDays(3), alerts.get(0).date());
    }

    @Test
    void membershipEndingLaterIsNotReported() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(5), TODAY.plusDays(25));
        visit(ana, TODAY);

        assertTrue(service.findByType(AlertType.EXPIRING).isEmpty());
    }

    @Test
    void membershipEndingTodayIsReportedWithZeroDays() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(29), TODAY);

        assertEquals(0, service.findByType(AlertType.EXPIRING).get(0).days());
    }

    @Test
    void renewedMembershipIsNotReported() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(27), TODAY.plusDays(2));
        subscribe(ana, TODAY.plusDays(3), TODAY.plusDays(32));

        assertTrue(service.findByType(AlertType.EXPIRING).isEmpty());
    }

    @Test
    void expiredMembershipsAndInactiveMembersAreNotReportedAsExpiring() {
        Member old = member("Luis Díaz");
        subscribe(old, TODAY.minusDays(60), TODAY.minusDays(30));
        Member left = member("Eva Ríos", LocalDate.of(1990, 1, 1), false);
        subscribe(left, TODAY.minusDays(10), TODAY.plusDays(2));

        assertTrue(service.findByType(AlertType.EXPIRING).isEmpty());
    }

    @Test
    void expiringMembershipsAreSortedByTheClosestFirst() {
        Member far = member("Zoe Mora");
        subscribe(far, TODAY.minusDays(24), TODAY.plusDays(6));
        Member near = member("Ana Gómez");
        subscribe(near, TODAY.minusDays(28), TODAY.plusDays(2));

        List<Alert> alerts = service.findByType(AlertType.EXPIRING);

        assertEquals(near.getId(), alerts.get(0).member().getId());
        assertEquals(far.getId(), alerts.get(1).member().getId());
    }

    // ----- members who stopped coming -----

    @Test
    void memberWithMembershipWhoLastCameTwentyDaysAgoIsInactive() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(40), TODAY.plusDays(20));
        visit(ana, TODAY.minusDays(30));
        visit(ana, TODAY.minusDays(20));

        List<Alert> alerts = service.findByType(AlertType.INACTIVE);

        assertEquals(1, alerts.size());
        assertEquals(20, alerts.get(0).days());
        assertEquals(TODAY.minusDays(20), alerts.get(0).date());
    }

    @Test
    void memberWhoCameRecentlyIsNotInactive() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(40), TODAY.plusDays(20));
        visit(ana, TODAY.minusDays(3));

        assertTrue(service.findByType(AlertType.INACTIVE).isEmpty());
    }

    @Test
    void memberWhoNeverCameCountsFromTheStartOfTheMembership() {
        Member recent = member("Ana Gómez");
        subscribe(recent, TODAY.minusDays(3), TODAY.plusDays(27));
        Member old = member("Luis Díaz");
        subscribe(old, TODAY.minusDays(20), TODAY.plusDays(10));

        List<Alert> alerts = service.findByType(AlertType.INACTIVE);

        assertEquals(1, alerts.size());
        assertEquals(old.getId(), alerts.get(0).member().getId());
        assertEquals(20, alerts.get(0).days());
    }

    @Test
    void memberWithoutMembershipIsNotInactive() {
        Member ana = member("Ana Gómez");
        visit(ana, TODAY.minusDays(50));

        assertTrue(service.findByType(AlertType.INACTIVE).isEmpty());
    }

    @Test
    void theMostAbsentMemberComesFirst() {
        Member short1 = member("Ana Gómez");
        subscribe(short1, TODAY.minusDays(15), TODAY.plusDays(15));
        Member long1 = member("Luis Díaz");
        subscribe(long1, TODAY.minusDays(25), TODAY.plusDays(5));

        List<Alert> alerts = service.findByType(AlertType.INACTIVE);

        assertEquals(long1.getId(), alerts.get(0).member().getId());
        assertEquals(short1.getId(), alerts.get(1).member().getId());
    }

    // ----- birthdays -----

    @Test
    void birthdayTodayIsReportedWithTheNewAge() {
        Member ana = member("Ana Gómez", LocalDate.of(2000, 11, 8), true);

        List<Alert> alerts = service.findByType(AlertType.BIRTHDAY);

        assertEquals(1, alerts.size());
        assertEquals(ana.getId(), alerts.get(0).member().getId());
        assertEquals(26, alerts.get(0).days());
        assertEquals(TODAY, alerts.get(0).date());
    }

    @Test
    void birthdayTomorrowIsNotReportedByDefault() {
        member("Ana Gómez", LocalDate.of(2000, 11, 9), true);

        assertTrue(service.findByType(AlertType.BIRTHDAY).isEmpty());
    }

    @Test
    void birthdayWindowReportsTheNextDays() {
        member("Ana Gómez", LocalDate.of(2000, 11, 9), true);
        member("Luis Díaz", LocalDate.of(1995, 11, 12), true);
        BirthdayRule rule = new BirthdayRule(members, 3);

        List<Alert> alerts = rule.evaluate(TODAY);

        assertEquals(1, alerts.size());
        assertEquals("Ana Gómez", alerts.get(0).member().getFullName());
    }

    @Test
    void birthdayWindowCrossesTheEndOfTheYear() {
        member("Ana Gómez", LocalDate.of(2000, 1, 2), true);
        BirthdayRule rule = new BirthdayRule(members, 5);

        List<Alert> alerts = rule.evaluate(LocalDate.of(2026, 12, 30));

        assertEquals(1, alerts.size());
        assertEquals(LocalDate.of(2027, 1, 2), alerts.get(0).date());
        assertEquals(27, alerts.get(0).days());
    }

    @Test
    void leapDayBirthdayIsCelebratedOnTwentyEighthOfFebruaryInCommonYears() {
        member("Ana Gómez", LocalDate.of(2000, 2, 29), true);
        BirthdayRule rule = new BirthdayRule(members, 0);

        assertEquals(1, rule.evaluate(LocalDate.of(2027, 2, 28)).size());
        assertTrue(rule.evaluate(LocalDate.of(2027, 3, 1)).isEmpty());
    }

    @Test
    void deactivatedMembersDoNotGetBirthdayAlerts() {
        member("Eva Ríos", LocalDate.of(1990, 11, 8), false);

        assertTrue(service.findByType(AlertType.BIRTHDAY).isEmpty());
    }

    // ----- the service as a whole -----

    @Test
    void alertsAreSortedByTypeAndCounted() {
        Member birthday = member("Zoe Mora", LocalDate.of(1999, 11, 8), true);
        Member inactive = member("Luis Díaz");
        subscribe(inactive, TODAY.minusDays(20), TODAY.plusDays(10));
        Member expiring = member("Ana Gómez");
        subscribe(expiring, TODAY.minusDays(28), TODAY.plusDays(2));
        visit(expiring, TODAY);

        List<Alert> all = service.findAll();

        assertEquals(3, all.size());
        assertEquals(List.of(AlertType.EXPIRING, AlertType.INACTIVE, AlertType.BIRTHDAY),
                all.stream().map(Alert::type).toList());
        assertEquals(3, service.count());
        assertEquals(Map.of(AlertType.EXPIRING, 1L, AlertType.INACTIVE, 1L, AlertType.BIRTHDAY, 1L),
                service.countByType());
        assertEquals(birthday.getId(), service.findByType(AlertType.BIRTHDAY).get(0).member().getId());
        assertEquals(inactive.getId(), service.findByType(AlertType.INACTIVE).get(0).member().getId());
    }

    @Test
    void withoutMembersThereAreNoAlertsAndEveryTypeCountsZero() {
        assertTrue(service.findAll().isEmpty());
        assertEquals(0, service.count());
        assertEquals(Map.of(AlertType.EXPIRING, 0L, AlertType.INACTIVE, 0L, AlertType.BIRTHDAY, 0L),
                service.countByType());
    }

    @Test
    void aNewRuleIsAddedWithoutChangingTheService() {
        Member ana = member("Ana Gómez");
        AlertRule custom = day -> List.of(new Alert(AlertType.BIRTHDAY, ana, 1, day));
        AlertService custom1 = new AlertService(List.of(custom), CLOCK);

        assertEquals(1, custom1.count());
    }
}
