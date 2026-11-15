package com.nextfit.service.portal;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.AccessResult.Outcome;
import com.nextfit.service.AttendanceService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * Builds the summary that a member sees after an entry or an exit. It reuses what the portal of the
 * member already gathers (routine, membership, streak, best hours) and only adds how long the member
 * stayed inside.
 */
public class VisitSummaryService {

    private final Function<Member, Optional<PortalSummary>> portal;
    private final AttendanceService attendance;

    /**
     * @param portal     gives what the portal shows for a member
     * @param attendance gives the visits, to know how long the member stayed
     */
    public VisitSummaryService(Function<Member, Optional<PortalSummary>> portal, AttendanceService attendance) {
        this.portal = portal;
        this.attendance = attendance;
    }

    /**
     * Builds the summary of an access.
     *
     * @param result what the access check answered
     * @return the summary, or empty when somebody was denied or the member is unknown
     */
    public Optional<VisitSummary> of(AccessResult result) {
        if (!result.granted() || result.member() == null) {
            return Optional.empty();
        }
        Member member = result.member();
        Optional<PortalSummary> summary = portal.apply(member);
        if (summary.isEmpty()) {
            return Optional.empty();
        }
        PortalSummary data = summary.get();
        boolean entering = result.outcome() == Outcome.ENTRY;
        return Optional.of(new VisitSummary(
                entering ? VisitSummary.Kind.ENTRY : VisitSummary.Kind.EXIT,
                data.workout(), data.membership(),
                entering ? Optional.empty() : lastStay(member.getId()),
                data.achievements().trainingDays(), data.achievements().streak().current(),
                entering ? data.bestTimes() : Optional.empty()));
    }

    /** How long the last closed visit lasted. */
    private Optional<Duration> lastStay(long memberId) {
        return attendance.findByMember(memberId).stream()
                .filter(record -> record.getCheckOut().isPresent())
                .max(Comparator.comparing((AttendanceRecord record) -> record.getCheckOut().get()))
                .map(record -> {
                    LocalDateTime out = record.getCheckOut().get();
                    Duration stay = Duration.between(record.getCheckIn(), out);
                    return stay.isNegative() ? Duration.ZERO : stay;
                });
    }
}
