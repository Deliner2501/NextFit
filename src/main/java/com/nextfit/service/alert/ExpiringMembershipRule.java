package com.nextfit.service.alert;

import com.nextfit.model.Member;
import com.nextfit.model.Subscription;
import com.nextfit.service.MemberService;
import com.nextfit.service.SubscriptionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the members whose membership ends within a few days and who have not
 * renewed yet. A member whose next subscription already starts the day after
 * the current one ends is not reported, because there is nothing to remind.
 */
public class ExpiringMembershipRule implements AlertRule {

    private final SubscriptionService subscriptions;
    private final MemberService members;
    private final int days;

    /**
     * Creates the rule.
     *
     * @param subscriptions the service that knows the subscriptions
     * @param members       the service that knows the members
     * @param days          how many days ahead to look
     */
    public ExpiringMembershipRule(SubscriptionService subscriptions, MemberService members, int days) {
        this.subscriptions = subscriptions;
        this.members = members;
        this.days = days;
    }

    @Override
    public List<Alert> evaluate(LocalDate today) {
        Map<Long, Member> activeMembers = new LinkedHashMap<>();
        for (Member member : members.findAll()) {
            if (member.isActive()) {
                activeMembers.put(member.getId(), member);
            }
        }
        Map<Long, Alert> byMember = new LinkedHashMap<>();
        for (Subscription subscription : subscriptions.findAll()) {
            Member member = activeMembers.get(subscription.getMemberId());
            if (member == null || !subscription.isActiveOn(today)) {
                continue;
            }
            long left = subscription.daysLeftOn(today);
            boolean renewed = subscriptions.hasActiveSubscription(member.getId(),
                    subscription.getEndDate().plusDays(1));
            if (left <= days && !renewed) {
                Alert alert = new Alert(AlertType.EXPIRING, member, left, subscription.getEndDate());
                byMember.merge(member.getId(), alert,
                        (first, second) -> first.days() <= second.days() ? first : second);
            }
        }
        List<Alert> result = new ArrayList<>(byMember.values());
        result.sort(Comparator.comparingLong(Alert::days));
        return result;
    }
}
