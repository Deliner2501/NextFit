package com.nextfit.service.alert;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.model.Subscription;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.service.SubscriptionService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the members who pay a membership but do not come to the gym. The days
 * without visiting are counted from the last visit, or from the day the current
 * membership started when the member has not visited since then, so a person
 * who has just renewed is not reported by mistake.
 */
public class InactiveMemberRule implements AlertRule {

    private final AttendanceService attendance;
    private final SubscriptionService subscriptions;
    private final MemberService members;
    private final int inactiveDays;

    /**
     * Creates the rule.
     *
     * @param attendance    the service that knows the visits
     * @param subscriptions the service that knows the subscriptions
     * @param members       the service that knows the members
     * @param inactiveDays  how many days without visiting make a member inactive
     */
    public InactiveMemberRule(AttendanceService attendance, SubscriptionService subscriptions,
                              MemberService members, int inactiveDays) {
        this.attendance = attendance;
        this.subscriptions = subscriptions;
        this.members = members;
        this.inactiveDays = inactiveDays;
    }

    @Override
    public List<Alert> evaluate(LocalDate today) {
        Map<Long, LocalDate> lastVisit = lastVisitByMember();
        Map<Long, LocalDate> membershipStart = currentMembershipStartByMember(today);
        List<Alert> result = new ArrayList<>();
        for (Member member : members.findAll()) {
            LocalDate start = membershipStart.get(member.getId());
            if (!member.isActive() || start == null) {
                continue;
            }
            LocalDate since = latest(lastVisit.get(member.getId()), start);
            long away = ChronoUnit.DAYS.between(since, today);
            if (away >= inactiveDays) {
                result.add(new Alert(AlertType.INACTIVE, member, away, since));
            }
        }
        result.sort(Comparator.comparingLong(Alert::days).reversed());
        return result;
    }

    private Map<Long, LocalDate> lastVisitByMember() {
        Map<Long, LocalDate> last = new HashMap<>();
        for (AttendanceRecord record : attendance.findAll()) {
            last.merge(record.getMemberId(), record.getCheckIn().toLocalDate(), InactiveMemberRule::latest);
        }
        return last;
    }

    private Map<Long, LocalDate> currentMembershipStartByMember(LocalDate today) {
        Map<Long, LocalDate> starts = new HashMap<>();
        for (Subscription subscription : subscriptions.findAll()) {
            if (subscription.isActiveOn(today)) {
                starts.merge(subscription.getMemberId(), subscription.getStartDate(), InactiveMemberRule::latest);
            }
        }
        return starts;
    }

    private static LocalDate latest(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        return first.isAfter(second) ? first : second;
    }
}
