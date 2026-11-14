package com.nextfit.service.churn;

import com.nextfit.exceptions.InvalidDataException;

import java.util.Optional;

/**
 * Adds risk when a member with a membership stops visiting. Up to a few days away
 * is normal and adds nothing; from there the points grow in a straight line until
 * they reach the maximum.
 */
public class DaysAwayRule implements ChurnRule {

    private final int maxPoints;
    private final int freeDays;
    private final int fullDays;

    /** Creates the rule with 40 points, free up to 3 days and the maximum at 14 days. */
    public DaysAwayRule() {
        this(40, 3, 14);
    }

    /**
     * Creates the rule.
     *
     * @param maxPoints the most points the rule can add
     * @param freeDays  the days away that add nothing
     * @param fullDays  the days away at which the rule adds its maximum
     * @throws InvalidDataException when the maximum days are not more than the free days
     */
    public DaysAwayRule(int maxPoints, int freeDays, int fullDays) {
        if (fullDays <= freeDays) {
            throw new InvalidDataException("días sin venir", "el tope debe ser mayor que los días libres");
        }
        this.maxPoints = maxPoints;
        this.freeDays = freeDays;
        this.fullDays = fullDays;
    }

    @Override
    public Optional<ChurnFactor> assess(MemberActivity activity) {
        long away = activity.daysAway();
        if (away <= freeDays) {
            return Optional.empty();
        }
        double share = Math.min(1.0, (double) (away - freeDays) / (fullDays - freeDays));
        int points = (int) Math.round(maxPoints * share);
        return Optional.of(new ChurnFactor(points, "Lleva " + away + " días sin venir"));
    }
}