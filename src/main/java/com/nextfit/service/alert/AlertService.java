package com.nextfit.service.alert;

import com.nextfit.service.AttendanceService;
import com.nextfit.service.DashboardService;
import com.nextfit.service.MemberService;
import com.nextfit.service.SubscriptionService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Gathers the alerts for the staff: memberships about to end, members who stopped
 * coming and birthdays. It only calculates them; it does not draw them or send
 * them, so the same alerts can feed a bell in the top bar, an alert center or a
 * WhatsApp message. Every kind of alert is an {@link AlertRule}, and the clock is
 * injected so tests can choose the day.
 */
public class AlertService {

    /** Days ahead in which a membership is reported as about to end. */
    public static final int EXPIRING_DAYS = DashboardService.SOON_DAYS;
    /** Days without visiting after which a member with a membership is reported. */
    public static final int INACTIVE_DAYS = 14;
    /** Days ahead in which a birthday is reported; 0 means only today. */
    public static final int BIRTHDAY_DAYS_AHEAD = 0;

    private final List<AlertRule> rules;
    private final Clock clock;

    /**
     * Creates the service with the rules it must ask.
     *
     * @param rules the rules, in the order their alerts are kept inside each type
     * @param clock the source of today's date
     */
    public AlertService(List<AlertRule> rules, Clock clock) {
        this.rules = List.copyOf(rules);
        this.clock = clock;
    }

    /**
     * Creates the service with the three usual rules.
     *
     * @param subscriptions the service that knows the subscriptions
     * @param attendance    the service that knows the visits
     * @param members       the service that knows the members
     * @param clock         the source of today's date
     * @return the service
     */
    public static AlertService withStandardRules(SubscriptionService subscriptions,
                                                 AttendanceService attendance,
                                                 MemberService members, Clock clock) {
        return new AlertService(List.of(
                new ExpiringMembershipRule(subscriptions, members, EXPIRING_DAYS),
                new InactiveMemberRule(attendance, subscriptions, members, INACTIVE_DAYS),
                new BirthdayRule(members, BIRTHDAY_DAYS_AHEAD)), clock);
    }

    /**
     * Returns every alert of today, the most important type first.
     *
     * @return the alerts
     */
    public List<Alert> findAll() {
        LocalDate today = LocalDate.now(clock);
        return rules.stream()
                .flatMap(rule -> rule.evaluate(today).stream())
                .sorted(Comparator.comparing(Alert::type))
                .toList();
    }

    /**
     * Returns the alerts of one type.
     *
     * @param type the type wanted
     * @return the alerts of that type
     */
    public List<Alert> findByType(AlertType type) {
        return findAll().stream().filter(alert -> alert.type() == type).toList();
    }

    /**
     * Counts every alert of today, for the badge of a bell.
     *
     * @return the number of alerts
     */
    public int count() {
        return findAll().size();
    }

    /**
     * Counts the alerts of each type. Every type is present, with zero when it has none.
     *
     * @return the number of alerts by type
     */
    public Map<AlertType, Long> countByType() {
        Map<AlertType, Long> counts = new EnumMap<>(AlertType.class);
        for (AlertType type : AlertType.values()) {
            counts.put(type, 0L);
        }
        for (Alert alert : findAll()) {
            counts.merge(alert.type(), 1L, Long::sum);
        }
        return counts;
    }
}
