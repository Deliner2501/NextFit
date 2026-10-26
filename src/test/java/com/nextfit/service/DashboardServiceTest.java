package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.Subscription;
import com.nextfit.service.BillingFakeRepositories.FakePaymentRepository;
import com.nextfit.service.BillingFakeRepositories.FakePlanRepository;
import com.nextfit.service.BillingFakeRepositories.FakeSubscriptionRepository;
import com.nextfit.service.FakeRepositories.FakeAttendanceRepository;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import com.nextfit.validation.MembershipPlanValidator;
import com.nextfit.validation.PaymentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private FakeSubscriptionRepository subscriptionRepository;
    private FakePaymentRepository paymentRepository;
    private FakeAttendanceRepository attendanceRepository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        // Today is Tuesday 27 October 2026.
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZONE);
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(new FakePlanRepository(),
                new MembershipPlanValidator());
        subscriptionRepository = new FakeSubscriptionRepository();
        paymentRepository = new FakePaymentRepository();
        attendanceRepository = new FakeAttendanceRepository();
        SubscriptionService subscriptions = new SubscriptionService(subscriptionRepository, plans, members, clock);
        PaymentService payments = new PaymentService(paymentRepository, subscriptions, plans,
                new PaymentValidator(), clock);
        AttendanceService attendance = new AttendanceService(members, attendanceRepository, subscriptions, clock);
        service = new DashboardService(subscriptions, payments, attendance, clock);

        MembershipPlan monthly = plans.create(MembershipPlanServiceTest.newPlan("Mensual", 30, 80_000));
        MembershipPlan weekly = plans.create(MembershipPlanServiceTest.newPlan("Semanal", 7, 25_000));
        Member a = members.register(FakeRepositories.newMember("10650001"));
        Member b = members.register(FakeRepositories.newMember("10650002"));
        Member c = members.register(FakeRepositories.newMember("10650003"));
        Member d = members.register(FakeRepositories.newMember("10650004"));
        Member e = members.register(FakeRepositories.newMember("10650005"));

        // active (29 days left) and ending soon (6 days left), both paid today
        payments.sellMembership(a.getId(), monthly.getId(), null, PaymentMethod.CASH, null, "admin");
        payments.sellMembership(b.getId(), weekly.getId(), null, PaymentMethod.CARD, null, "admin");
        // cancelled
        subscriptions.cancel(subscriptions.subscribe(c.getId(), monthly.getId(), null).getId());
        // upcoming
        subscriptions.subscribe(d.getId(), monthly.getId(), LocalDate.of(2026, 11, 1));
        // expired in September
        subscriptionRepository.save(new Subscription(0, e.getId(), monthly.getId(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), false));
        // a payment from September
        paymentRepository.save(new Payment(0, 99, e.getId(), LocalDateTime.of(2026, 9, 5, 9, 0),
                PaymentMethod.CASH, 50_000, new NoDiscount(), "admin"));

        // two entries today (Tue), one yesterday (Mon), one a week ago (Tue), one outside the 28 days
        enter(a.getId(), LocalDateTime.of(2026, 10, 27, 7, 0));
        enter(b.getId(), LocalDateTime.of(2026, 10, 27, 8, 0));
        enter(a.getId(), LocalDateTime.of(2026, 10, 26, 7, 0));
        enter(a.getId(), LocalDateTime.of(2026, 10, 20, 7, 0));
        enter(a.getId(), LocalDateTime.of(2026, 9, 27, 7, 0));
    }

    private void enter(long memberId, LocalDateTime when) {
        attendanceRepository.save(new AttendanceRecord(0, memberId, when, when.plusHours(1)));
    }

    @Test
    void summaryCountsMembershipsByState() {
        DashboardService.Summary summary = service.summary();

        assertEquals(LocalDate.of(2026, 10, 27), summary.today());
        assertEquals(2, summary.activeSubscriptions());
        assertEquals(1, summary.expiringSoon());
        assertEquals(1, summary.upcoming());
        assertEquals(1, summary.expired());
        assertEquals(1, summary.cancelled());
    }

    @Test
    void summaryAddsUpTheIncomeOfTheMonthAndOfEachMonth() {
        DashboardService.Summary summary = service.summary();

        assertEquals(105_000, summary.incomeThisMonth());
        assertEquals(DashboardService.INCOME_MONTHS, summary.incomeByMonth().size());
        assertEquals(50_000L, summary.incomeByMonth().get(YearMonth.of(2026, 9)));
        assertEquals(105_000L, summary.incomeByMonth().get(YearMonth.of(2026, 10)));
        assertEquals(0L, summary.incomeByMonth().get(YearMonth.of(2026, 5)));
    }

    @Test
    void summaryCountsEntriesTodayAndByWeekdayInTheLastFourWeeks() {
        DashboardService.Summary summary = service.summary();

        assertEquals(2, summary.checkInsToday());
        assertEquals(7, summary.checkInsByWeekday().size());
        assertEquals(3L, summary.checkInsByWeekday().get(DayOfWeek.TUESDAY));
        assertEquals(1L, summary.checkInsByWeekday().get(DayOfWeek.MONDAY));
        assertEquals(0L, summary.checkInsByWeekday().get(DayOfWeek.SUNDAY));
    }
}
