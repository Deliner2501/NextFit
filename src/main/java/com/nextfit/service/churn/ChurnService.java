package com.nextfit.service.churn;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.Subscription;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.SubscriptionService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calculates which members may stop coming. It gathers what is known about each
 * member with a current membership and asks every {@link ChurnRule}; the points
 * are added and the result is a score from 0 to 100. Every rule is a class of its
 * own, so a new sign of abandonment does not change this service, and the clock is
 * injected so tests can choose the day.
 */
public class ChurnService {

    private static final int MAX_SCORE = 100;

    private final AttendanceService attendance;
    private final SubscriptionService subscriptions;
    private final MemberService members;
    private final List<ChurnRule> rules;
    private final Clock clock;

    /**
     * Creates the service with the rules it must ask.
     *
     * @param attendance    the service that knows the visits
     * @param subscriptions the service that knows the subscriptions
     * @param members       the service that knows the members
     * @param rules         the rules that give points
     * @param clock         the source of today's date
     */
    public ChurnService(AttendanceService attendance, SubscriptionService subscriptions,
                        MemberService members, List<ChurnRule> rules, Clock clock) {
        this.attendance = attendance;
        this.subscriptions = subscriptions;
        this.members = members;
        this.rules = List.copyOf(rules);
        this.clock = clock;
    }

    /**
     * Creates the service with the three usual rules.
     *
     * @param attendance    the service that knows the visits
     * @param subscriptions the service that knows the subscriptions
     * @param members       the service that knows the members
     * @param clock         the source of today's date
     * @return the service
     */
    public static ChurnService withStandardRules(AttendanceService attendance, SubscriptionService subscriptions,
                                                 MemberService members, Clock clock) {
        return new ChurnService(attendance, subscriptions, members,
                List.of(new FrequencyDropRule(), new DaysAwayRule(), new ExpiringSoonRule()), clock);
    }

    /**
     * Returns the risk of every active member with a current membership, the highest first.
     *
     * @return the risks
     */
    public List<ChurnRisk> findAll() {
        LocalDate today = LocalDate.now(clock);
        Map<Long, List<LocalDate>> visits = visitDaysByMember();
        List<Subscription> allSubscriptions = subscriptions.findAll();
        List<ChurnRisk> result = new ArrayList<>();
        for (Member member : members.findAll()) {
            if (member.isActive()) {
                activityOf(member, today, visits, allSubscriptions)
                        .map(activity -> riskOf(member, activity))
                        .ifPresent(result::add);
            }
        }
        result.sort(Comparator.comparingInt(ChurnRisk::score).reversed()
                .thenComparing(risk -> risk.member().getFullName()));
        return result;
    }

    /**
     * Returns the members whose risk reaches a level.
     *
     * @param minimum the lowest level wanted
     * @return the risks at that level or above, the highest first
     */
    public List<ChurnRisk> findAtLeast(RiskLevel minimum) {
        return findAll().stream().filter(risk -> risk.level().compareTo(minimum) >= 0).toList();
    }

    /**
     * Returns the risk of one member.
     *
     * @param memberId the member
     * @return the risk, or empty when the member is inactive or has no current membership
     */
    public Optional<ChurnRisk> findFor(long memberId) {
        return findAll().stream().filter(risk -> risk.member().getId() == memberId).findFirst();
    }

    /**
     * Counts the members of each level. Every level is present, with zero when it has none.
     *
     * @return the number of members by level
     */
    public Map<RiskLevel, Long> countByLevel() {
        Map<RiskLevel, Long> counts = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            counts.put(level, 0L);
        }
        for (ChurnRisk risk : findAll()) {
            counts.merge(risk.level(), 1L, Long::sum);
        }
        return counts;
    }

    private ChurnRisk riskOf(Member member, MemberActivity activity) {
        List<ChurnFactor> factors = rules.stream()
                .map(rule -> rule.assess(activity))
                .flatMap(Optional::stream)
                .filter(factor -> factor.points() > 0)
                .sorted(Comparator.comparingInt(ChurnFactor::points).reversed())
                .toList();
        int score = Math.min(MAX_SCORE, factors.stream().mapToInt(ChurnFactor::points).sum());
        return new ChurnRisk(member, score, RiskLevel.of(score),
                factors.stream().map(ChurnFactor::reason).toList());
    }

    // EXPLICACIÓN: solo se evalúa a quien tiene una membresía vigente hoy; sin membresía no hay
    // nada que abandonar. Se usa la membresía más reciente si tiene varias.
    private Optional<MemberActivity> activityOf(Member member, LocalDate today,
                                                Map<Long, List<LocalDate>> visits,
                                                List<Subscription> allSubscriptions) {
        List<Subscription> current = allSubscriptions.stream()
                .filter(s -> s.getMemberId() == member.getId() && s.isActiveOn(today))
                .toList();
        if (current.isEmpty()) {
            return Optional.empty();
        }
        LocalDate start = current.stream().map(Subscription::getStartDate).max(Comparator.naturalOrder()).orElseThrow();
        LocalDate end = current.stream().map(Subscription::getEndDate).max(Comparator.naturalOrder()).orElseThrow();
        boolean renewed = subscriptions.hasActiveSubscription(member.getId(), end.plusDays(1));
        return Optional.of(new MemberActivity(today, visits.getOrDefault(member.getId(), List.of()),
                start, end, renewed));
    }

    private Map<Long, List<LocalDate>> visitDaysByMember() {
        Map<Long, List<LocalDate>> visits = new HashMap<>();
        for (AttendanceRecord record : attendance.findAll()) {
            visits.computeIfAbsent(record.getMemberId(), id -> new ArrayList<>())
                    .add(record.getCheckIn().toLocalDate());
        }
        return visits;
    }
}