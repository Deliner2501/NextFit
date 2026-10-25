package com.nextfit.service;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Discount;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.NoDiscount;
import com.nextfit.model.Payment;
import com.nextfit.model.PaymentMethod;
import com.nextfit.model.Subscription;
import com.nextfit.persistence.PaymentRepository;
import com.nextfit.validation.PaymentValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rules of the sale of memberships and of the money reports built from the
 * payments. Selling a membership creates the subscription and registers its
 * payment as one operation.
 */
public class PaymentService {

    private final PaymentRepository payments;
    private final SubscriptionService subscriptions;
    private final MembershipPlanService plans;
    private final PaymentValidator validator;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param payments      the payment storage
     * @param subscriptions the subscription rules
     * @param plans         the plan rules
     * @param validator     the checker of payment data
     * @param clock         the source of the current date and time
     */
    public PaymentService(PaymentRepository payments, SubscriptionService subscriptions,
                          MembershipPlanService plans, PaymentValidator validator, Clock clock) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Sells a membership: subscribes the member and registers the payment. If
     * the payment cannot be stored, the subscription that was just created is
     * cancelled so the member is never left with a membership nobody charged.
     *
     * @param memberId       the member who buys
     * @param planId         the plan being bought
     * @param requestedStart the wished first day, or null for today
     * @param method         how the member pays
     * @param discount       the discount to apply, or null for none
     * @param registeredBy   the user name of the staff member who sells
     * @return the subscription, the payment and the plan, ready for the receipt
     * @throws InvalidDataException      when the method or the seller is missing, or the start is in the past
     * @throws ResourceNotFoundException when the member or the plan does not exist
     */
    public MembershipSale sellMembership(long memberId, long planId, LocalDate requestedStart,
                                         PaymentMethod method, Discount discount, String registeredBy) {
        validator.validateSale(method, registeredBy);
        MembershipPlan plan = plans.findById(planId);
        Subscription subscription = subscriptions.subscribe(memberId, planId, requestedStart);
        Discount applied = discount == null ? new NoDiscount() : discount;
        try {
            Payment saved = payments.save(new Payment(0, subscription.getId(), memberId,
                    LocalDateTime.now(clock), method, plan.getPrice(), applied, registeredBy.trim()));
            return new MembershipSale(subscription, saved, plan);
        } catch (RuntimeException e) {
            try {
                subscriptions.cancel(subscription.getId());
            } catch (RuntimeException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        }
    }

    /**
     * Looks a payment up by identifier.
     *
     * @param id the identifier
     * @return the payment
     * @throws ResourceNotFoundException when it does not exist
     */
    public Payment findById(long id) {
        return payments.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("pago", String.valueOf(id)));
    }

    /**
     * Returns every payment, the newest first.
     *
     * @return the payments
     */
    public List<Payment> findAll() {
        return payments.findAll().stream().sorted(newestFirst()).toList();
    }

    /**
     * Returns the payments of one member, the newest first.
     *
     * @param memberId the identifier of the member
     * @return the payments
     */
    public List<Payment> findByMember(long memberId) {
        return payments.findByMemberId(memberId).stream().sorted(newestFirst()).toList();
    }

    /**
     * Returns the latest payments.
     *
     * @param limit how many to return at most
     * @return the newest payments
     */
    public List<Payment> findRecent(int limit) {
        return payments.findAll().stream().sorted(newestFirst()).limit(limit).toList();
    }

    /**
     * Adds up what was collected between two days, both included.
     *
     * @param from the first day
     * @param to   the last day
     * @return the total in pesos
     */
    public long revenueBetween(LocalDate from, LocalDate to) {
        return payments.findAll().stream()
                .filter(p -> isBetween(p, from, to))
                .mapToLong(Payment::getTotal)
                .sum();
    }

    /**
     * Adds up what was collected in each of the last months, the current one
     * included. Months without payments appear with zero, so a chart has no gaps.
     *
     * @param months how many months to return
     * @return the totals in pesos, from the oldest month to the current one
     */
    public Map<YearMonth, Long> revenueByMonth(int months) {
        YearMonth current = YearMonth.now(clock);
        Map<YearMonth, Long> result = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            result.put(current.minusMonths(i), 0L);
        }
        for (Payment p : payments.findAll()) {
            YearMonth month = YearMonth.from(p.getPaidAt());
            result.computeIfPresent(month, (m, total) -> total + p.getTotal());
        }
        return result;
    }

    /**
     * Adds up what was collected with each payment method between two days,
     * both included. Every method appears, even with zero.
     *
     * @param from the first day
     * @param to   the last day
     * @return the totals in pesos
     */
    public Map<PaymentMethod, Long> revenueByMethod(LocalDate from, LocalDate to) {
        Map<PaymentMethod, Long> result = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            result.put(method, 0L);
        }
        for (Payment p : payments.findAll()) {
            if (isBetween(p, from, to)) {
                result.merge(p.getMethod(), p.getTotal(), Long::sum);
            }
        }
        return result;
    }

    private static boolean isBetween(Payment payment, LocalDate from, LocalDate to) {
        LocalDate day = payment.getPaidAt().toLocalDate();
        return !day.isBefore(from) && !day.isAfter(to);
    }

    private static Comparator<Payment> newestFirst() {
        return Comparator.comparing(Payment::getPaidAt).reversed().thenComparing(Comparator.comparingLong(Payment::getId).reversed());
    }
}
