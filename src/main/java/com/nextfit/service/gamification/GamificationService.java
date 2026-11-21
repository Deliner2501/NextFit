package com.nextfit.service.gamification;

import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the attendance of the members into progress: training days, weekly streaks,
 * badges and a monthly ranking. It only calculates; it draws nothing and changes nothing,
 * so the member file, the phone portal and the TV screen can all show the same numbers.
 * A "training day" is a different calendar day with at least one entry, so coming twice in
 * a day counts once. The clock is injected so tests can choose the day.
 */
public class GamificationService {

    /** Different days a week needs for the week to count in a streak. */
    public static final int MIN_DAYS_PER_WEEK = 3;
    /** How many members the ranking shows when no other number is asked. */
    public static final int DEFAULT_RANKING_SIZE = 10;

    private final MemberService members;
    private final AttendanceService attendance;
    private final Clock clock;

    /**
     * @param members    the service that knows the members
     * @param attendance the service that knows the visits
     * @param clock      tells which day is today
     */
    public GamificationService(MemberService members, AttendanceService attendance, Clock clock) {
        this.members = members;
        this.attendance = attendance;
        this.clock = clock;
    }

    /**
     * Calculates the progress of one member.
     *
     * @param memberId the id of the member
     * @return the training days, the streak and the badges earned
     * @throws ResourceNotFoundException when the member does not exist
     */
    public Achievements achievementsOf(long memberId) {
        Member member = members.findById(memberId);
        Set<LocalDate> days = new HashSet<>();
        for (AttendanceRecord record : attendance.findByMember(memberId)) {
            days.add(record.getCheckIn().toLocalDate());
        }
        Streak streak = StreakCalculator.calculate(days, LocalDate.now(clock), MIN_DAYS_PER_WEEK);
        List<Badge> badges = Arrays.stream(Badge.values())
                .filter(badge -> badge.isEarnedBy(days.size(), streak.best()))
                .toList();
        return new Achievements(member, days.size(), streak, badges);
    }

    /**
     * Builds the ranking of the current month.
     *
     * @param limit how many members to show, at least 1
     * @return the members with most training days, best first
     * @throws IllegalArgumentException when the limit is less than 1
     */
    public List<RankingEntry> monthlyRanking(int limit) {
        return monthlyRanking(YearMonth.now(clock), limit);
    }

    /**
     * Builds the ranking of a month. Only active members who trained that month appear;
     * members with the same days share the position and are sorted by name.
     *
     * @param month the month to rank
     * @param limit how many members to show, at least 1
     * @return the members with most training days in the month, best first
     * @throws IllegalArgumentException when the limit is less than 1
     */
    public List<RankingEntry> monthlyRanking(YearMonth month, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("El ranking debe mostrar al menos un socio");
        }
        Map<Long, Set<LocalDate>> daysByMember = new HashMap<>();
        for (AttendanceRecord record : attendance.findAll()) {
            LocalDate day = record.getCheckIn().toLocalDate();
            if (YearMonth.from(day).equals(month)) {
                daysByMember.computeIfAbsent(record.getMemberId(), id -> new HashSet<>()).add(day);
            }
        }
        List<Member> ranked = members.findAll().stream()
                .filter(Member::isActive)
                .filter(member -> daysByMember.containsKey(member.getId()))
                .sorted(Comparator
                        .comparingInt((Member member) -> daysByMember.get(member.getId()).size()).reversed()
                        .thenComparing(Member::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<RankingEntry> ranking = new ArrayList<>();
        int position = 0;
        int previousDays = -1;
        for (int i = 0; i < ranked.size() && ranking.size() < limit; i++) {
            Member member = ranked.get(i);
            int days = daysByMember.get(member.getId()).size();
            if (days != previousDays) {
                position = i + 1;
                previousDays = days;
            }
            ranking.add(new RankingEntry(position, member, days));
        }
        return ranking;
    }
}
