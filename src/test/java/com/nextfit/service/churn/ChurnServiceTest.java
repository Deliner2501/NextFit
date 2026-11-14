package com.nextfit.service.churn;

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
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the churn risk with real CSV files in a temporary folder and a fixed
 * clock: today is 13 November 2026.
 */
class ChurnServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    // Days ago of the visits: the last 2 weeks are 0 to 13 days ago, the 4 weeks before are 14 to 41.
    private static final int[] NOW_4_PER_WEEK = {0, 2, 4, 6, 7, 9, 11, 13};
    private static final int[] BEFORE_4_PER_WEEK = {14, 16, 18, 20, 21, 23, 25, 27, 28, 30, 32, 34, 35, 37, 39, 41};

    private CsvMemberRepository memberRepository;
    private CsvSubscriptionRepository subscriptionRepository;
    private CsvAttendanceRepository attendanceRepository;
    private MemberService members;
    private SubscriptionService subscriptions;
    private AttendanceService attendance;
    private ChurnService service;
    private int nextDocument = 20000;

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
        service = ChurnService.withStandardRules(attendance, subscriptions, members, CLOCK);
    }

    private Member member(String name, boolean active) {
        return memberRepository.save(new Member(0, name, String.valueOf(nextDocument++), "3001234567",
                "socio@example.com", LocalDate.of(1998, 3, 1), LocalDate.of(2026, 1, 5), "",
                "Mamá 3110000000", active));
    }

    private Member member(String name) {
        return member(name, true);
    }

    private void subscribe(Member member, LocalDate start, LocalDate end) {
        subscriptionRepository.save(new Subscription(0, member.getId(), 1, start, end, false));
    }

    private void visit(Member member, int... daysAgo) {
        for (int days : daysAgo) {
            LocalDate day = TODAY.minusDays(days);
            attendanceRepository.save(new AttendanceRecord(0, member.getId(),
                    LocalDateTime.of(day, LocalTime.of(18, 0)), LocalDateTime.of(day, LocalTime.of(19, 0))));
        }
    }

    // A member with a long membership who keeps coming 4 times a week.
    private Member steadyMember(String name) {
        Member member = member(name);
        subscribe(member, TODAY.minusDays(90), TODAY.plusDays(30));
        visit(member, NOW_4_PER_WEEK);
        visit(member, BEFORE_4_PER_WEEK);
        return member;
    }

    @Test
    void aMemberWhoKeepsComingHasNoRisk() {
        steadyMember("Ana Gómez");

        ChurnRisk risk = service.findAll().get(0);

        assertEquals(0, risk.score());
        assertEquals(RiskLevel.LOW, risk.level());
        assertTrue(risk.reasons().isEmpty());
    }

    @Test
    void aMemberWhoStoppedComingHasAHighRiskWithTheReasons() {
        Member luis = member("Luis Pérez");
        subscribe(luis, TODAY.minusDays(90), TODAY.plusDays(30));
        visit(luis, BEFORE_4_PER_WEEK);   // he came 4 times a week and nothing in the last 2 weeks

        ChurnRisk risk = service.findAll().get(0);

        assertEquals(80, risk.score());
        assertEquals(RiskLevel.HIGH, risk.level());
        assertEquals("Escribirle o llamarlo hoy", risk.action());
        assertEquals(List.of("Pasó de 4 a 0 visitas por semana", "Lleva 14 días sin venir"), risk.reasons());
    }

    @Test
    void aMembershipAboutToEndAddsRiskUntilItIsRenewed() {
        Member ana = member("Ana Gómez");
        subscribe(ana, TODAY.minusDays(27), TODAY.plusDays(3));
        visit(ana, 0, 2, 4, 6, 7, 9, 11, 13, 14, 16, 18, 20, 21, 23, 25, 27);

        ChurnRisk before = service.findAll().get(0);
        assertEquals(11, before.score());
        assertEquals(List.of("Su membresía vence en 3 días y no ha renovado"), before.reasons());

        subscribe(ana, TODAY.plusDays(4), TODAY.plusDays(33));   // she renews

        assertEquals(0, service.findAll().get(0).score());
    }

    @Test
    void membersWithoutACurrentMembershipAreNotEvaluated() {
        Member expired = member("Sin Plan");
        subscribe(expired, TODAY.minusDays(60), TODAY.minusDays(1));
        member("Nunca Compró");

        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void inactiveMembersAreNotEvaluated() {
        Member left = member("Dado de Baja", false);
        subscribe(left, TODAY.minusDays(60), TODAY.plusDays(30));

        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void theMembersAreSortedFromTheHighestRisk() {
        steadyMember("Ana Gómez");
        Member luis = member("Luis Pérez");
        subscribe(luis, TODAY.minusDays(90), TODAY.plusDays(30));
        visit(luis, BEFORE_4_PER_WEEK);

        List<ChurnRisk> risks = service.findAll();

        assertEquals("Luis Pérez", risks.get(0).member().getFullName());
        assertEquals("Ana Gómez", risks.get(1).member().getFullName());
    }

    @Test
    void findAtLeastKeepsOnlyTheMembersFromThatLevel() {
        steadyMember("Ana Gómez");
        Member luis = member("Luis Pérez");
        subscribe(luis, TODAY.minusDays(90), TODAY.plusDays(30));
        visit(luis, BEFORE_4_PER_WEEK);

        List<ChurnRisk> high = service.findAtLeast(RiskLevel.HIGH);

        assertEquals(1, high.size());
        assertEquals("Luis Pérez", high.get(0).member().getFullName());
    }

    @Test
    void findForReturnsTheRiskOfOneMemberOrNothing() {
        Member ana = steadyMember("Ana Gómez");
        Member none = member("Nunca Compró");

        assertTrue(service.findFor(ana.getId()).isPresent());
        assertEquals(Optional.empty(), service.findFor(none.getId()));
    }

    @Test
    void countByLevelHasEveryLevelEvenWhenEmpty() {
        steadyMember("Ana Gómez");

        Map<RiskLevel, Long> counts = service.countByLevel();

        assertEquals(1L, counts.get(RiskLevel.LOW));
        assertEquals(0L, counts.get(RiskLevel.MEDIUM));
        assertEquals(0L, counts.get(RiskLevel.HIGH));
    }

    @Test
    void theScoreNeverPassesOneHundred() {
        steadyMember("Ana Gómez");
        ChurnRule heavy = activity -> Optional.of(new ChurnFactor(60, "Muy pesado"));
        ChurnService capped = new ChurnService(attendance, subscriptions, members, List.of(heavy, heavy), CLOCK);

        assertEquals(100, capped.findAll().get(0).score());
    }
}