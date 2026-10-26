package com.nextfit.service;

import com.nextfit.model.Subscription;
import com.nextfit.model.SubscriptionStatus;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

/**
 * Gathers the numbers shown in the dashboard from the membership, payment and
 * attendance services. It only reads: it never changes any data. The screen
 * receives one {@link Summary} and decides how to draw it.
 */
public class DashboardService {

    /** Days before the end of a membership in which it counts as "ending soon". */
    public static final int SOON_DAYS = 7;
    /** Months of income shown in the income chart. */
    public static final int INCOME_MONTHS = 6;
    /** Days of attendance used for the weekday chart (four weeks). */
    public static final int ATTENDANCE_DAYS = 28;

    /**
     * Everything the dashboard shows.
     *
     * @param today               the day the numbers were worked out
     * @param activeSubscriptions memberships that give access today
     * @param expiringSoon        active memberships that end within {@link #SOON_DAYS} days
     * @param upcoming            memberships that have not started yet
     * @param expired             memberships that already ended
     * @param cancelled           memberships cancelled before their end
     * @param incomeThisMonth     pesos collected from the first of the month until today
     * @param checkInsToday       entries registered today
     * @param incomeByMonth       pesos collected in each of the last months, oldest first
     * @param checkInsByWeekday   entries in the last four weeks, grouped by day of the week
     */
    public record Summary(LocalDate today, long activeSubscriptions, long expiringSoon, long upcoming,
                          long expired, long cancelled, long incomeThisMonth, long checkInsToday,
                          Map<YearMonth, Long> incomeByMonth, Map<DayOfWeek, Long> checkInsByWeekday) {
    }

    private final SubscriptionService subscriptions;
    private final PaymentService payments;
    private final AttendanceService attendance;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param subscriptions the membership rules
     * @param payments      the payment rules and reports
     * @param attendance    the entry and exit rules
     * @param clock         the source of today's date
     */
    public DashboardService(SubscriptionService subscriptions, PaymentService payments,
                            AttendanceService attendance, Clock clock) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.attendance = attendance;
        this.clock = clock;
    }

    /**
     * Works out every number of the dashboard for today.
     *
     * @return the summary
     */
    public Summary summary() {
        LocalDate today = LocalDate.now(clock);
        Map<SubscriptionStatus, Long> byStatus = countByStatus();
        return new Summary(today,
                byStatus.get(SubscriptionStatus.ACTIVE),
                subscriptions.findExpiringWithin(SOON_DAYS).size(),
                byStatus.get(SubscriptionStatus.UPCOMING),
                byStatus.get(SubscriptionStatus.EXPIRED),
                byStatus.get(SubscriptionStatus.CANCELLED),
                payments.revenueBetween(today.withDayOfMonth(1), today),
                attendance.countCheckInsOn(today),
                payments.revenueByMonth(INCOME_MONTHS),
                checkInsByWeekday(today));
    }

    private Map<SubscriptionStatus, Long> countByStatus() {
        Map<SubscriptionStatus, Long> counts = new EnumMap<>(SubscriptionStatus.class);
        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            counts.put(status, 0L);
        }
        for (Subscription s : subscriptions.findAll()) {
            counts.merge(subscriptions.statusOf(s), 1L, Long::sum);
        }
        return counts;
    }

    private Map<DayOfWeek, Long> checkInsByWeekday(LocalDate today) {
        Map<DayOfWeek, Long> counts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            counts.put(day, 0L);
        }
        for (int i = 0; i < ATTENDANCE_DAYS; i++) {
            LocalDate day = today.minusDays(i);
            counts.merge(day.getDayOfWeek(), attendance.countCheckInsOn(day), Long::sum);
        }
        return counts;
    }
}
