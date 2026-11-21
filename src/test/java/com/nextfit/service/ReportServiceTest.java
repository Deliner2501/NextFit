package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.PaymentMethod;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    private FakeAttendanceRepository attendanceRepo;
    private PaymentService payments;
    private ReportService service;
    private CashCloseService cashCloses;
    private Member member;
    private MembershipPlan monthly;

    @BeforeEach
    void setUp() {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        MembershipPlanService plans = new MembershipPlanService(new FakePlanRepository(), new MembershipPlanValidator());
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T15:00:00Z"), ZONE);
        SubscriptionService subscriptions = new SubscriptionService(new FakeSubscriptionRepository(), plans, members, clock);
        payments = new PaymentService(new FakePaymentRepository(), subscriptions, plans, new PaymentValidator(), clock);
        attendanceRepo = new FakeAttendanceRepository();
        AttendanceService attendance = new AttendanceService(members, attendanceRepo, subscriptions, clock);
        cashCloses = new CashCloseService(payments, members);
        service = new ReportService(cashCloses, members, subscriptions, attendance);
        member = members.register(FakeRepositories.newMember("1065123456"));
        monthly = plans.create(MembershipPlanServiceTest.newPlan("Mensual", 30, 80_000));
    }

    @Test
    void paymentsTableHasOneRowPerPaymentAndMatchingColumns() {
        payments.sellMembership(member.getId(), monthly.getId(), null, PaymentMethod.CARD, null, "admin");

        ReportTable table = service.payments(TODAY, TODAY);

        assertEquals(1, table.rows().size());
        assertEquals(table.headers().size(), table.rows().get(0).size());
        assertEquals("Tarjeta", table.rows().get(0).get(4));
        assertEquals(80_000L, table.rows().get(0).get(7));
        assertEquals(ColumnType.MONEY, table.types().get(7));
    }

    @Test
    void cashCloseSheetsHaveASummaryAndThePayments() {
        payments.sellMembership(member.getId(), monthly.getId(), null, PaymentMethod.CASH, null, "admin");

        java.util.List<ReportTable> sheets = service.cashCloseSheets(cashCloses.closeOf(TODAY).withCountedCash(70_000));

        assertEquals(2, sheets.size());
        assertEquals("Total recaudado", sheets.get(0).rows().get(0).get(0));
        assertEquals(80_000L, sheets.get(0).rows().get(0).get(2));
        assertEquals(-10_000L, sheets.get(0).rows().get(sheets.get(0).rows().size() - 1).get(2));
        assertEquals(1, sheets.get(1).rows().size());
        assertTrue(sheets.get(1).rows().get(0).get(1).toString().startsWith("27/10/2026"));
    }

    @Test
    void paymentsTableIsEmptyOutsideTheRange() {
        payments.sellMembership(member.getId(), monthly.getId(), null, PaymentMethod.CASH, null, "admin");

        assertEquals(0, service.payments(TODAY.plusDays(1), TODAY.plusDays(5)).rows().size());
    }

    @Test
    void membersTableShowsTheMembershipEnd() {
        assertEquals("Sin membresía", service.members().rows().get(0).get(6));

        payments.sellMembership(member.getId(), monthly.getId(), null, PaymentMethod.CASH, null, "admin");

        assertEquals("25/11/2026", service.members().rows().get(0).get(6));
    }

    @Test
    void attendanceTableShowsMinutesAndOpenVisits() {
        attendanceRepo.save(new AttendanceRecord(0, member.getId(),
                LocalDateTime.of(2026, 10, 27, 9, 0), LocalDateTime.of(2026, 10, 27, 10, 15)));
        attendanceRepo.save(new AttendanceRecord(0, member.getId(),
                LocalDateTime.of(2026, 10, 27, 18, 0), null));

        ReportTable table = service.attendance(TODAY, TODAY);

        assertEquals(2, table.rows().size());
        assertEquals(75L, table.rows().get(0).get(4));
        assertEquals("Dentro", table.rows().get(1).get(3));
        assertEquals(0L, table.rows().get(1).get(4));
    }

    @Test
    void aTableWithMismatchedRowsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ReportTable("x", java.util.List.of("a", "b"),
                java.util.List.of(ColumnType.TEXT, ColumnType.TEXT), java.util.List.of(java.util.List.<Object>of("only one"))));
    }
}
