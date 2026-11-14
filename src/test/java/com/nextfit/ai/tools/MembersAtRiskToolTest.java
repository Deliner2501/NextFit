package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.InvalidDataException;
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
import com.nextfit.service.churn.ChurnService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the tool "members at risk" with real CSV files in a temporary folder; today is 13 November 2026. */
class MembersAtRiskToolTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);
    private static final int[] NOW_4_PER_WEEK = {0, 2, 4, 6, 7, 9, 11, 13};
    private static final int[] BEFORE_4_PER_WEEK = {14, 16, 18, 20, 21, 23, 25, 27, 28, 30, 32, 34, 35, 37, 39, 41};
    private static final AiUser ADMIN = new AiUser(AiAudience.ADMIN, 0, "admin");

    private CsvMemberRepository memberRepository;
    private CsvSubscriptionRepository subscriptionRepository;
    private CsvAttendanceRepository attendanceRepository;
    private MembersAtRiskTool tool;
    private int nextDocument = 20000;

    @BeforeEach
    void setUp(@TempDir Path folder) {
        memberRepository = new CsvMemberRepository(folder.resolve("members.csv"));
        subscriptionRepository = new CsvSubscriptionRepository(folder.resolve("subscriptions.csv"));
        attendanceRepository = new CsvAttendanceRepository(folder.resolve("attendance.csv"));
        MemberService members = new MemberService(memberRepository, new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(
                new CsvMembershipPlanRepository(folder.resolve("plans.csv")), new MembershipPlanValidator());
        SubscriptionService subscriptions = new SubscriptionService(subscriptionRepository, plans, members, CLOCK);
        AttendanceService attendance = new AttendanceService(members, attendanceRepository, subscriptions, CLOCK);
        tool = new MembersAtRiskTool(ChurnService.withStandardRules(attendance, subscriptions, members, CLOCK));
    }

    private Member member(String name) {
        Member member = memberRepository.save(new Member(0, name, String.valueOf(nextDocument++), "3001234567",
                "socio@example.com", LocalDate.of(1998, 3, 1), LocalDate.of(2026, 1, 5), "",
                "Mamá 3110000000", true));
        subscriptionRepository.save(new Subscription(0, member.getId(), 1,
                TODAY.minusDays(90), TODAY.plusDays(30), false));
        return member;
    }

    private void visit(Member member, int... daysAgo) {
        for (int days : daysAgo) {
            LocalDate day = TODAY.minusDays(days);
            attendanceRepository.save(new AttendanceRecord(0, member.getId(),
                    LocalDateTime.of(day, LocalTime.of(18, 0)), LocalDateTime.of(day, LocalTime.of(19, 0))));
        }
    }

    private void aSteadyMemberAndALeavingOne() {
        Member ana = member("Ana Gómez");
        visit(ana, NOW_4_PER_WEEK);
        visit(ana, BEFORE_4_PER_WEEK);
        Member luis = member("Luis Pérez");
        visit(luis, BEFORE_4_PER_WEEK);
    }

    @Test
    void listsTheMemberWhoMayLeaveWithReasonAndAction() {
        aSteadyMemberAndALeavingOne();

        String text = tool.run(Map.of(), ADMIN);

        assertTrue(text.contains("Luis Pérez"));
        assertTrue(text.contains("alto"));
        assertTrue(text.contains("80/100"));
        assertTrue(text.contains("Acción sugerida: Escribirle o llamarlo hoy"));
        assertFalse(text.contains("Ana Gómez"));
    }

    @Test
    void doesNotGivePhonesOrDocuments() {
        aSteadyMemberAndALeavingOne();

        String text = tool.run(Map.of(), ADMIN);

        assertFalse(text.contains("3001234567"));
        assertFalse(text.contains("20001"));
        assertFalse(text.contains("socio@example.com"));
        assertFalse(text.contains("3110000000"));
    }

    @Test
    void saysSoWhenNobodyIsAtRisk() {
        Member ana = member("Ana Gómez");
        visit(ana, NOW_4_PER_WEEK);
        visit(ana, BEFORE_4_PER_WEEK);

        assertTrue(tool.run(Map.of(), ADMIN).startsWith("Ningún socio"));
    }

    @Test
    void theLevelAndTheLimitAreApplied() {
        aSteadyMemberAndALeavingOne();
        Member carla = member("Carla Ruiz");
        visit(carla, BEFORE_4_PER_WEEK);

        String onlyOne = tool.run(Map.of("limit", 1L), ADMIN);
        String high = tool.run(Map.of("level", "alto"), ADMIN);

        assertTrue(onlyOne.contains("2 socio(s)"));
        assertTrue(onlyOne.contains("se muestran los 1 primeros"));
        assertEquals(1, onlyOne.lines().filter(line -> line.startsWith("- ")).count());
        assertTrue(high.contains("2 socio(s)"));
    }

    @Test
    void aWrongLevelOrLimitIsRejectedWithAClearMessage() {
        assertThrows(InvalidDataException.class, () -> tool.run(Map.of("level", "ninguno"), ADMIN));
        assertThrows(InvalidDataException.class, () -> tool.run(Map.of("limit", "muchos"), ADMIN));
    }

    @Test
    void onlyAdminAndReceptionMayUseIt() {
        assertTrue(tool.allowedFor(AiAudience.ADMIN));
        assertTrue(tool.allowedFor(AiAudience.RECEPTION));
        assertFalse(tool.allowedFor(AiAudience.TRAINER));
        assertFalse(tool.allowedFor(AiAudience.MEMBER));
    }
}