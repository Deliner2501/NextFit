package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the cash close of a day from the payments. It only reads: it never
 * changes a payment, so closing the cash can be repeated as many times as needed.
 */
public class CashCloseService {

    private static final String UNKNOWN = "—";

    private final PaymentService payments;
    private final MemberService members;

    /**
     * Creates the service.
     *
     * @param payments the source of payments and their plans
     * @param members  the source of member names
     */
    public CashCloseService(PaymentService payments, MemberService members) {
        this.payments = payments;
        this.members = members;
    }

    /**
     * Builds the close of a day.
     *
     * @param day the day to close
     * @return the close, with no cash counted
     */
    public CashClose closeOf(LocalDate day) {
        List<CashClose.Line> lines = linesBetween(day, day);
        Map<PaymentMethod, Long> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            byMethod.put(method, 0L);
        }
        long total = 0;
        long discounts = 0;
        for (CashClose.Line line : lines) {
            byMethod.merge(line.method(), line.total(), Long::sum);
            total += line.total();
            discounts += line.discount();
        }
        return new CashClose(day, lines, byMethod, total, discounts, -1);
    }

    /**
     * Lists the payments made between two days, both included.
     *
     * @param from the first day
     * @param to   the last day
     * @return the payments, oldest first
     */
    public List<CashClose.Line> linesBetween(LocalDate from, LocalDate to) {
        return payments.findAll().stream()
                .filter(p -> !p.getPaidAt().toLocalDate().isBefore(from) && !p.getPaidAt().toLocalDate().isAfter(to))
                .sorted(Comparator.comparing(Payment::getPaidAt))
                .map(this::toLine)
                .toList();
    }

    private CashClose.Line toLine(Payment p) {
        return new CashClose.Line(p.getReceiptNumber(), p.getPaidAt().withSecond(0).withNano(0),
                memberName(p.getMemberId()), planName(p), p.getMethod(), p.getSubtotal(), p.getDiscountAmount(),
                p.getTotal(), p.getRegisteredBy());
    }

    private String memberName(long memberId) {
        try {
            Member member = members.findById(memberId);
            return member.getFullName();
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    private String planName(Payment p) {
        try {
            return payments.findSale(p.getId()).plan().getName();
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }
}
