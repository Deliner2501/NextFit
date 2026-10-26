package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.Subscription;
import com.nextfit.model.SubscriptionStatus;
import com.nextfit.persistence.SubscriptionRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rules of the subscriptions: who can subscribe, when a subscription starts and
 * whether a member may use the gym today. It also fulfils the
 * {@link SubscriptionChecker} contract that other modules depend on. The clock
 * is injected so the rules can be tested on any date.
 */
public class SubscriptionService implements SubscriptionChecker {

    private final SubscriptionRepository subscriptions;
    private final MembershipPlanService plans;
    private final MemberService members;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param subscriptions the subscription storage
     * @param plans         the plan rules
     * @param members       the member rules
     * @param clock         the source of today's date
     */
    public SubscriptionService(SubscriptionRepository subscriptions, MembershipPlanService plans,
                               MemberService members, Clock clock) {
        this.subscriptions = subscriptions;
        this.plans = plans;
        this.members = members;
        this.clock = clock;
    }

    /**
     * Subscribes a member to a plan. When the member still has a subscription
     * running, the new one starts the day after it ends, so a renewal never
     * wastes days.
     *
     * @param memberId       the member who subscribes
     * @param planId         the plan to buy
     * @param requestedStart the wished first day, or null for today
     * @return the stored subscription
     * @throws ResourceNotFoundException when the member or the plan does not exist
     * @throws BusinessRuleException     when the member is inactive or the plan is no longer sold
     * @throws InvalidDataException      when the wished start is in the past
     */
    public Subscription subscribe(long memberId, long planId, LocalDate requestedStart) {
        Member member = members.findById(memberId);
        if (!member.isActive()) {
            throw new BusinessRuleException("El socio está inactivo y no puede adquirir una membresía");
        }
        MembershipPlan plan = plans.findById(planId);
        if (!plan.isActive()) {
            throw new BusinessRuleException("El plan '" + plan.getName() + "' ya no está disponible");
        }
        LocalDate today = today();
        LocalDate start = requestedStart == null ? today : requestedStart;
        if (start.isBefore(today)) {
            throw new InvalidDataException("fecha de inicio", "no puede ser anterior a hoy");
        }
        start = firstFreeDay(memberId, start);
        return subscriptions.save(new Subscription(0, memberId, planId, start, plan.endDateFor(start), false));
    }

    /**
     * Tells whether the member may use the gym today.
     *
     * @param memberId the identifier of the member
     * @return true when one of the member's subscriptions is active today
     */
    @Override
    public boolean hasActiveSubscription(long memberId) {
        return hasActiveSubscription(memberId, today());
    }

    /**
     * Tells whether the member may use the gym on a given day.
     *
     * @param memberId the identifier of the member
     * @param day      the day to check
     * @return true when one of the member's subscriptions is active that day
     */
    public boolean hasActiveSubscription(long memberId, LocalDate day) {
        return subscriptions.findByMemberId(memberId).stream().anyMatch(s -> s.isActiveOn(day));
    }

    /**
     * Returns the subscription the member is using today.
     *
     * @param memberId the identifier of the member
     * @return the active subscription, or empty when there is none
     */
    public Optional<Subscription> findCurrentFor(long memberId) {
        LocalDate today = today();
        return subscriptions.findByMemberId(memberId).stream().filter(s -> s.isActiveOn(today)).findFirst();
    }

    /**
     * Returns every subscription, the newest first.
     *
     * @return the subscriptions
     */
    public List<Subscription> findAll() {
        return subscriptions.findAll().stream()
                .sorted(Comparator.comparing(Subscription::getStartDate).reversed()
                        .thenComparing(Comparator.comparingLong(Subscription::getId).reversed()))
                .toList();
    }

    /**
     * Works out the situation of a subscription today.
     *
     * @param subscription the subscription
     * @return upcoming, active, expired or cancelled
     */
    public SubscriptionStatus statusOf(Subscription subscription) {
        return subscription.statusOn(today());
    }

    /**
     * Counts the days left of a subscription from today.
     *
     * @param subscription the subscription
     * @return the remaining days, zero when it is not active today
     */
    public long daysLeftOf(Subscription subscription) {
        return subscription.daysLeftOn(today());
    }

    /**
     * Returns every subscription of a member, the newest first.
     *
     * @param memberId the identifier of the member
     * @return the subscriptions
     */
    public List<Subscription> findByMember(long memberId) {
        return subscriptions.findByMemberId(memberId).stream()
                .sorted(Comparator.comparing(Subscription::getStartDate).reversed())
                .toList();
    }

    /**
     * Looks a subscription up by identifier.
     *
     * @param id the identifier
     * @return the subscription
     * @throws ResourceNotFoundException when it does not exist
     */
    public Subscription findById(long id) {
        return subscriptions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("suscripción", String.valueOf(id)));
    }

    /**
     * Cancels a subscription that has not ended yet.
     *
     * @param id the identifier of the subscription
     * @return the cancelled subscription
     * @throws ResourceNotFoundException when it does not exist
     * @throws BusinessRuleException     when it is already cancelled or already expired
     */
    public Subscription cancel(long id) {
        Subscription subscription = findById(id);
        SubscriptionStatus status = subscription.statusOn(today());
        if (status == SubscriptionStatus.CANCELLED) {
            throw new BusinessRuleException("La suscripción ya está cancelada");
        }
        if (status == SubscriptionStatus.EXPIRED) {
            throw new BusinessRuleException("No se puede cancelar una suscripción que ya venció");
        }
        return subscriptions.save(subscription.cancel());
    }

    /**
     * Returns the active subscriptions that end soon, the closest first. It is
     * the list the reception uses to remind members to renew.
     *
     * @param days how many days ahead to look
     * @return the subscriptions that end within that many days
     */
    public List<Subscription> findExpiringWithin(int days) {
        LocalDate today = today();
        return subscriptions.findAll().stream()
                .filter(s -> s.isActiveOn(today) && s.daysLeftOn(today) <= days)
                .sorted(Comparator.comparing(Subscription::getEndDate))
                .toList();
    }

    /**
     * Counts the subscriptions that are active today.
     *
     * @return the number of active subscriptions
     */
    public long countActive() {
        LocalDate today = today();
        return subscriptions.findAll().stream().filter(s -> s.isActiveOn(today)).count();
    }

    private LocalDate firstFreeDay(long memberId, LocalDate wished) {
        LocalDate start = wished;
        for (Subscription s : subscriptions.findByMemberId(memberId)) {
            if (!s.isCancelled() && !s.getEndDate().isBefore(start)) {
                start = s.getEndDate().plusDays(1);
            }
        }
        return start;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
