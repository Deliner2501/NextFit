package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.Subscription;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.nextfit.service.ColumnType.MONEY;
import static com.nextfit.service.ColumnType.NUMBER;
import static com.nextfit.service.ColumnType.TEXT;

/**
 * Builds the tables of the reports (payments, members, attendance) as
 * {@link ReportTable}. It only reads and never draws: the exporters decide the
 * file format.
 */
public class ReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CashCloseService cashClose;
    private final MemberService members;
    private final SubscriptionService subscriptions;
    private final AttendanceService attendance;

    /**
     * Creates the service.
     *
     * @param cashClose     the source of payment lines
     * @param members       the source of members
     * @param subscriptions the source of memberships
     * @param attendance    the source of entries and exits
     */
    public ReportService(CashCloseService cashClose, MemberService members, SubscriptionService subscriptions,
                         AttendanceService attendance) {
        this.cashClose = cashClose;
        this.members = members;
        this.subscriptions = subscriptions;
        this.attendance = attendance;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    /**
     * Builds the payments report.
     *
     * @param from the first day
     * @param to   the last day
     * @return the table
     */
    public ReportTable payments(LocalDate from, LocalDate to) {
        return paymentsTable("Pagos " + DAY.format(from) + " a " + DAY.format(to), cashClose.linesBetween(from, to));
    }

    private static ReportTable paymentsTable(String title, List<CashClose.Line> lines) {
        List<List<Object>> rows = new ArrayList<>();
        for (CashClose.Line line : lines) {
            rows.add(List.of(line.receipt(), MOMENT.format(line.paidAt()), line.member(), line.plan(),
                    line.method().getLabel(), line.subtotal(), line.discount(), line.total(), text(line.cashier())));
        }
        return new ReportTable(title,
                List.of("Recibo", "Fecha", "Socio", "Plan", "Método", "Subtotal", "Descuento", "Total", "Cajero"),
                List.of(TEXT, TEXT, TEXT, TEXT, TEXT, MONEY, MONEY, MONEY, TEXT), rows);
    }

    /**
     * Builds the two sheets of the Excel file of a cash close: a summary and the payments of the day.
     *
     * @param close the close of one day
     * @return the summary table followed by the payments table
     */
    public List<ReportTable> cashCloseSheets(CashClose close) {
        List<List<Object>> summary = new ArrayList<>();
        summary.add(List.of("Total recaudado", (long) close.count(), close.total()));
        for (PaymentMethod method : PaymentMethod.values()) {
            long count = close.lines().stream().filter(l -> l.method() == method).count();
            summary.add(List.of("   " + method.getLabel(), count, close.byMethod().getOrDefault(method, 0L)));
        }
        long discounted = close.lines().stream().filter(l -> l.discount() > 0).count();
        summary.add(List.of("Descuentos otorgados", discounted, close.discounts()));
        summary.add(List.of("Efectivo esperado", "", close.expectedCash()));
        summary.add(List.of("Efectivo contado", "", close.isCounted() ? close.countedCash() : 0L));
        summary.add(List.of("Diferencia (contado - esperado)", "", close.isCounted() ? close.difference() : 0L));
        ReportTable sheet = new ReportTable("Resumen " + DAY.format(close.day()),
                List.of("Concepto", "Pagos", "Valor"), List.of(TEXT, NUMBER, MONEY), summary);
        return List.of(sheet, paymentsTable("Pagos " + DAY.format(close.day()), close.lines()));
    }

    /**
     * Builds the members report, with the end of the current membership of each one.
     *
     * @return the table, sorted by name
     */
    public ReportTable members() {
        List<List<Object>> rows = new ArrayList<>();
        members.findAll().stream().sorted(Comparator.comparing(Member::getFullName, String.CASE_INSENSITIVE_ORDER))
                .forEach(m -> rows.add(List.of(m.getFullName(), m.getDocumentNumber(), text(m.getPhone()), text(m.getEmail()),
                        m.isActive() ? "Activo" : "Inactivo", DAY.format(m.getJoinDate()),
                        subscriptions.findCurrentFor(m.getId()).map(Subscription::getEndDate).map(DAY::format)
                                .orElse("Sin membresía"))));
        return new ReportTable("Socios", List.of("Nombre", "Documento", "Teléfono", "Correo", "Estado", "Ingreso",
                "Membresía hasta"), List.of(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT), rows);
    }

    /**
     * Builds the attendance report.
     *
     * @param from the first day
     * @param to   the last day
     * @return the table, oldest entry first
     */
    public ReportTable attendance(LocalDate from, LocalDate to) {
        Map<Long, String> names = members.findAll().stream()
                .collect(Collectors.toMap(Member::getId, Member::getFullName, (a, b) -> a));
        Map<Long, String> documents = members.findAll().stream()
                .collect(Collectors.toMap(Member::getId, Member::getDocumentNumber, (a, b) -> a));
        List<List<Object>> rows = new ArrayList<>();
        attendance.findAll().stream()
                .filter(r -> !r.getCheckIn().toLocalDate().isBefore(from) && !r.getCheckIn().toLocalDate().isAfter(to))
                .sorted(Comparator.comparing(AttendanceRecord::getCheckIn))
                .forEach(r -> {
                    LocalDateTime out = r.getCheckOut().orElse(null);
                    rows.add(List.of(names.getOrDefault(r.getMemberId(), "—"),
                            documents.getOrDefault(r.getMemberId(), "—"), MOMENT.format(r.getCheckIn()),
                            out == null ? "Dentro" : MOMENT.format(out),
                            out == null ? 0L : Math.max(0, Duration.between(r.getCheckIn(), out).toMinutes())));
                });
        return new ReportTable("Asistencia " + DAY.format(from) + " a " + DAY.format(to),
                List.of("Socio", "Documento", "Entrada", "Salida", "Minutos"),
                List.of(TEXT, TEXT, TEXT, TEXT, NUMBER), rows);
    }
}
