package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Subscription;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out the insights of the analytics screen: when the gym is busy, how long
 * people stay, how many memberships are renewed and which members with a valid
 * membership have stopped coming. It only reads, and the screen only draws the
 * {@link Report} it returns.
 */
public class AnalyticsService {

    /** Weeks of entries used for the heatmap. */
    public static final int HEATMAP_WEEKS = 8;
    /** Days without a visit after which an active member is "at risk". */
    public static final int AT_RISK_DAYS = 14;
    /** Days back in which an ended membership counts for the renewal rate. */
    public static final int RENEWAL_WINDOW_DAYS = 60;

    /**
     * An active member who has stopped coming.
     *
     * @param memberId  the member
     * @param lastVisit the day of the last entry, or null when the member never came
     * @param daysSince days since the last entry, or -1 when the member never came
     */
    public record AtRisk(long memberId, LocalDate lastVisit, long daysSince) {
    }

    /**
     * Everything the analytics screen shows.
     *
     * @param heatmap          entries by weekday and hour in the last {@link #HEATMAP_WEEKS} weeks
     * @param averageStayMin   average minutes between entry and exit, 0 when there is no closed visit
     * @param renewalRate      share of ended memberships that were renewed, from 0 to 1, or -1 when none ended
     * @param renewalBase      how many ended memberships the rate is based on
     * @param atRisk           members to contact, the ones who never came or came longest ago first
     */
    public record Report(OccupancyHeatmap heatmap, long averageStayMin, double renewalRate, int renewalBase,
                         List<AtRisk> atRisk) {
    }

    private final AttendanceService attendance;
    private final SubscriptionService subscriptions;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param attendance    the source of entries and exits
     * @param subscriptions the source of memberships
     * @param clock         the source of today's date
     */
    public AnalyticsService(AttendanceService attendance, SubscriptionService subscriptions, Clock clock) {
        this.attendance = attendance;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /**
     * Works out the whole report for today.
     *
     * @return the report
     */
    public Report report() {
        LocalDate today = LocalDate.now(clock);
        List<AttendanceRecord> records = attendance.findAll();
        List<Subscription> all = subscriptions.findAll();

        LocalDate heatmapFrom = today.minusWeeks(HEATMAP_WEEKS);
        List<AttendanceRecord> recent = records.stream()
                .filter(r -> !r.getCheckIn().toLocalDate().isBefore(heatmapFrom))
                .toList();

        int[] renewal = renewal(all, today);
        double rate = renewal[1] == 0 ? -1 : (double) renewal[0] / renewal[1];
        return new Report(OccupancyHeatmap.of(recent), averageStay(records), rate, renewal[1],
                atRisk(all, records, today));
    }

    private static long averageStay(List<AttendanceRecord> records) {
        long totalMinutes = 0;
        long visits = 0;
        for (AttendanceRecord r : records) {
            if (r.getCheckOut().isPresent()) {
                long minutes = Duration.between(r.getCheckIn(), r.getCheckOut().get()).toMinutes();
                if (minutes > 0) {
                    totalMinutes += minutes;
                    visits++;
                }
            }
        }
        return visits == 0 ? 0 : Math.round((double) totalMinutes / visits);
    }

    /** Returns {renewed, eligible} for the memberships that ended in the renewal window. */
    private static int[] renewal(List<Subscription> all, LocalDate today) {
        LocalDate from = today.minusDays(RENEWAL_WINDOW_DAYS);
        int renewed = 0;
        int eligible = 0;
        for (Subscription ended : all) {
            boolean inWindow = !ended.isCancelled()
                    && ended.getEndDate().isBefore(today) && !ended.getEndDate().isBefore(from);
            if (!inWindow) {
                continue;
            }
            eligible++;
            boolean hasLater = all.stream().anyMatch(other -> other.getMemberId() == ended.getMemberId()
                    && !other.isCancelled()
                    && other.getStartDate().isAfter(ended.getStartDate())
                    && !other.getStartDate().isAfter(today));
            if (hasLater) {
                renewed++;
            }
        }
        return new int[] {renewed, eligible};
    }

    private static List<AtRisk> atRisk(List<Subscription> all, List<AttendanceRecord> records, LocalDate today) {
        Map<Long, LocalDate> lastVisit = new HashMap<>();
        for (AttendanceRecord r : records) {
            lastVisit.merge(r.getMemberId(), r.getCheckIn().toLocalDate(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }
        LocalDate limit = today.minusDays(AT_RISK_DAYS);
        Set<Long> seen = new HashSet<>();
        List<AtRisk> result = new ArrayList<>();
        for (Subscription s : all) {
            // A member who joined less than two weeks ago has not had time to stop coming.
            if (!s.isActiveOn(today) || s.getStartDate().isAfter(limit) || !seen.add(s.getMemberId())) {
                continue;
            }
            LocalDate last = lastVisit.get(s.getMemberId());
            if (last == null || last.isBefore(limit)) {
                result.add(new AtRisk(s.getMemberId(), last,
                        last == null ? -1 : ChronoUnit.DAYS.between(last, today)));
            }
        }
        result.sort(Comparator.comparing(AtRisk::lastVisit, Comparator.nullsFirst(Comparator.naturalOrder())));
        return result;
    }
}
