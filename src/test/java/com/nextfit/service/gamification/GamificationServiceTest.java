package com.nextfit.service.gamification;

import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamificationServiceTest {

    // A Sunday: the current week goes from Monday 2 to Sunday 8 of November.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private static final LocalDate THIS_MONDAY = LocalDate.of(2026, 11, 2);
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private CsvMemberRepository memberRepository;
    private CsvAttendanceRepository attendanceRepository;
    private GamificationService service;
    private int nextDocument = 20000;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        memberRepository = new CsvMemberRepository(folder.resolve("members.csv"));
        attendanceRepository = new CsvAttendanceRepository(folder.resolve("attendance.csv"));
        MemberService members = new MemberService(memberRepository, new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        SubscriptionService subscriptions = new SubscriptionService(
                new CsvSubscriptionRepository(folder.resolve("subscriptions.csv")), plans, members, CLOCK);
        AttendanceService attendance = new AttendanceService(members, attendanceRepository, subscriptions, CLOCK);
        service = new GamificationService(members, attendance, CLOCK);
    }

    private Member member(String name, boolean active) {
        return memberRepository.save(new Member(0, name, String.valueOf(nextDocument++), "3001234567",
                "socio@example.com", LocalDate.of(1998, 3, 1), LocalDate.of(2026, 1, 5), "", "Mamá 3110000000", active));
    }

    private Member member(String name) {
        return member(name, true);
    }

    private void visit(Member member, LocalDate day, int hour) {
        attendanceRepository.save(new AttendanceRecord(0, member.getId(), LocalDateTime.of(day, LocalTime.of(hour, 0)),
                LocalDateTime.of(day, LocalTime.of(hour + 1, 0))));
    }

    private void visit(Member member, LocalDate day) {
        visit(member, day, 18);
    }

    // The member trains on the given number of days (Monday, Tuesday...) of each of the last weeks.
    private void trainWeeks(Member member, int weeks, int daysPerWeek) {
        for (int weeksAgo = 0; weeksAgo < weeks; weeksAgo++) {
            for (int i = 0; i < daysPerWeek; i++) {
                visit(member, THIS_MONDAY.minusWeeks(weeksAgo).plusDays(i));
            }
        }
    }

    // ----- achievements of a member -----

    @Test
    void aNewMemberHasNothingYet() {
        Member ana = member("Ana Gómez");

        Achievements achievements = service.achievementsOf(ana.getId());

        assertEquals(0, achievements.trainingDays());
        assertEquals(Streak.NONE, achievements.streak());
        assertTrue(achievements.badges().isEmpty());
        assertEquals(ana.getId(), achievements.member().getId());
    }

    @Test
    void theFirstVisitEarnsTheFirstBadge() {
        Member ana = member("Ana Gómez");
        visit(ana, TODAY.minusDays(1));

        Achievements achievements = service.achievementsOf(ana.getId());

        assertEquals(1, achievements.trainingDays());
        assertEquals(List.of(Badge.FIRST_STEP), achievements.badges());
    }

    @Test
    void twoVisitsInTheSameDayCountOnce() {
        Member ana = member("Ana Gómez");
        visit(ana, TODAY.minusDays(1), 7);
        visit(ana, TODAY.minusDays(1), 18);

        assertEquals(1, service.achievementsOf(ana.getId()).trainingDays());
    }

    @Test
    void tenDifferentDaysEarnTheSteadyBadge() {
        Member ana = member("Ana Gómez");
        for (int back = 1; back <= 10; back++) {
            visit(ana, TODAY.minusDays(back));
        }

        Achievements achievements = service.achievementsOf(ana.getId());

        assertEquals(10, achievements.trainingDays());
        assertEquals(List.of(Badge.FIRST_STEP, Badge.STEADY), achievements.badges());
    }

    @Test
    void fourWeeksMeetingTheGoalEarnTheMonthStreak() {
        Member ana = member("Ana Gómez");
        trainWeeks(ana, 4, 3);

        Achievements achievements = service.achievementsOf(ana.getId());

        assertEquals(new Streak(4, 4), achievements.streak());
        assertEquals(List.of(Badge.FIRST_STEP, Badge.STEADY, Badge.MONTH_STREAK), achievements.badges());
    }

    @Test
    void aBadgeIsKeptEvenWhenTheStreakIsBroken() {
        Member ana = member("Ana Gómez");
        // Four weeks in a row that ended five weeks ago, and nothing since.
        for (int weeksAgo = 5; weeksAgo <= 8; weeksAgo++) {
            for (int i = 0; i < 3; i++) {
                visit(ana, THIS_MONDAY.minusWeeks(weeksAgo).plusDays(i));
            }
        }

        Achievements achievements = service.achievementsOf(ana.getId());

        assertEquals(new Streak(0, 4), achievements.streak());
        assertTrue(achievements.badges().contains(Badge.MONTH_STREAK));
    }

    @Test
    void anUnknownMemberIsRefused() {
        assertThrows(ResourceNotFoundException.class, () -> service.achievementsOf(999));
    }

    // ----- monthly ranking -----

    @Test
    void theRankingPutsTheMemberWithMostDaysFirst() {
        Member ana = member("Ana Gómez");
        Member beto = member("Beto Ruiz");
        for (int day = 1; day <= 3; day++) {
            visit(ana, LocalDate.of(2026, 11, day));
        }
        for (int day = 1; day <= 6; day++) {
            visit(beto, LocalDate.of(2026, 11, day));
        }

        List<RankingEntry> ranking = service.monthlyRanking(NOVEMBER, 10);

        assertEquals(2, ranking.size());
        assertEquals(beto.getId(), ranking.get(0).member().getId());
        assertEquals(1, ranking.get(0).position());
        assertEquals(6, ranking.get(0).days());
        assertEquals(ana.getId(), ranking.get(1).member().getId());
        assertEquals(2, ranking.get(1).position());
    }

    @Test
    void membersWithTheSameDaysShareThePositionAndAreSortedByName() {
        Member carla = member("Carla Díaz");
        Member ana = member("Ana Gómez");
        Member beto = member("Beto Ruiz");
        for (int day = 1; day <= 4; day++) {
            visit(carla, LocalDate.of(2026, 11, day));
            visit(ana, LocalDate.of(2026, 11, day));
        }
        visit(beto, LocalDate.of(2026, 11, 1));

        List<RankingEntry> ranking = service.monthlyRanking(NOVEMBER, 10);

        assertEquals(List.of(ana.getId(), carla.getId(), beto.getId()),
                ranking.stream().map(entry -> entry.member().getId()).toList());
        assertEquals(List.of(1, 1, 3), ranking.stream().map(RankingEntry::position).toList());
    }

    @Test
    void theRankingOnlyCountsTheRequestedMonth() {
        Member ana = member("Ana Gómez");
        visit(ana, LocalDate.of(2026, 10, 30));
        visit(ana, LocalDate.of(2026, 10, 31));
        visit(ana, LocalDate.of(2026, 11, 3));

        assertEquals(1, service.monthlyRanking(NOVEMBER, 10).get(0).days());
        assertEquals(2, service.monthlyRanking(YearMonth.of(2026, 10), 10).get(0).days());
    }

    @Test
    void theRankingLeavesOutInactiveMembersAndThoseWithoutVisits() {
        Member ana = member("Ana Gómez");
        Member inactive = member("Inés Baja", false);
        member("Beto Ruiz");
        visit(ana, LocalDate.of(2026, 11, 3));
        visit(inactive, LocalDate.of(2026, 11, 3));

        List<RankingEntry> ranking = service.monthlyRanking(NOVEMBER, 10);

        assertEquals(1, ranking.size());
        assertEquals(ana.getId(), ranking.get(0).member().getId());
    }

    @Test
    void theRankingShowsOnlyTheRequestedNumberOfMembers() {
        for (int i = 1; i <= 4; i++) {
            visit(member("Socio " + i), LocalDate.of(2026, 11, i));
        }

        assertEquals(2, service.monthlyRanking(NOVEMBER, 2).size());
    }

    @Test
    void theRankingWithoutMonthUsesTheMonthOfTheClock() {
        Member ana = member("Ana Gómez");
        visit(ana, LocalDate.of(2026, 11, 3));
        visit(ana, LocalDate.of(2026, 10, 3));

        List<RankingEntry> ranking = service.monthlyRanking(5);

        assertEquals(1, ranking.size());
        assertEquals(1, ranking.get(0).days());
    }

    @Test
    void theRankingMustShowAtLeastOneMember() {
        assertThrows(IllegalArgumentException.class, () -> service.monthlyRanking(NOVEMBER, 0));
    }
}
