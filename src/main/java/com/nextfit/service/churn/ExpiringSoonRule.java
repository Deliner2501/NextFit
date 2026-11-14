package com.nextfit.service.churn;

import com.nextfit.exceptions.InvalidDataException;

import java.util.Optional;

/**
 * Adds risk when the membership of a member is about to end and he has not renewed
 * it: the fewer days are left, the more points. A member who already renewed adds
 * nothing, because there is nothing to remind.
 */
public class ExpiringSoonRule implements ChurnRule {

    private final int maxPoints;
    private final int days;

    /** Creates the rule with 20 points, looking 7 days ahead. */
    public ExpiringSoonRule() {
        this(20, 7);
    }

    /**
     * Creates the rule.
     *
     * @param maxPoints the most points the rule can add, on the last day
     * @param days      how many days ahead the rule looks
     * @throws InvalidDataException when the days are not greater than zero
     */
    public ExpiringSoonRule(int maxPoints, int days) {
        if (days <= 0) {
            throw new InvalidDataException("días", "debe ser mayor que cero");
        }
        this.maxPoints = maxPoints;
        this.days = days;
    }

    @Override
    public Optional<ChurnFactor> assess(MemberActivity activity) {
        long left = activity.daysLeft();
        if (activity.renewed() || left < 0 || left >= days) {
            return Optional.empty();
        }
        int points = (int) Math.round(maxPoints * (days - left) / (double) days);
        return Optional.of(new ChurnFactor(points, "Su membresía vence " + when(left) + " y no ha renovado"));
    }

    private static String when(long daysLeft) {
        if (daysLeft == 0) {
            return "hoy";
        }
        if (daysLeft == 1) {
            return "mañana";
        }
        return "en " + daysLeft + " días";
    }
}