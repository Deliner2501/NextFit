package com.nextfit.service.churn;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * What the churn rules need to know about one member: the days he visited and the
 * current membership. It is a plain value, so the rules can be tested without
 * services or files.
 *
 * @param today           the day being evaluated
 * @param visitDays       the days the member visited; repeated days are kept once
 * @param membershipStart the day the current membership started
 * @param membershipEnd   the last valid day of the current membership
 * @param renewed         true when the next membership already starts the day after this one ends
 */
public record MemberActivity(LocalDate today, List<LocalDate> visitDays, LocalDate membershipStart,
                             LocalDate membershipEnd, boolean renewed) {

    /** Keeps each visit day once and in order. */
    public MemberActivity {
        visitDays = visitDays.stream().distinct().sorted().toList();
    }

    /**
     * Counts the days with a visit in a range.
     *
     * @param from first day of the range, included
     * @param to   last day of the range, included
     * @return the number of days with a visit
     */
    public long visitsBetween(LocalDate from, LocalDate to) {
        return visitDays.stream().filter(day -> !day.isBefore(from) && !day.isAfter(to)).count();
    }

    /**
     * Returns the last day the member visited.
     *
     * @return the day, or empty when he has never visited
     */
    public Optional<LocalDate> lastVisit() {
        return visitDays.isEmpty() ? Optional.empty() : Optional.of(visitDays.get(visitDays.size() - 1));
    }

    /**
     * Counts the days without visiting. They are counted from the last visit, or from
     * the day the membership started when the member has not visited since then, so
     * a person who has just renewed is not seen as absent.
     *
     * @return the days away
     */
    public long daysAway() {
        LocalDate since = membershipStart;
        Optional<LocalDate> last = lastVisit();
        if (last.isPresent() && last.get().isAfter(since)) {
            since = last.get();
        }
        return ChronoUnit.DAYS.between(since, today);
    }

    /**
     * Counts the days left of the current membership.
     *
     * @return the days left, zero on the last day
     */
    public long daysLeft() {
        return ChronoUnit.DAYS.between(today, membershipEnd);
    }
}