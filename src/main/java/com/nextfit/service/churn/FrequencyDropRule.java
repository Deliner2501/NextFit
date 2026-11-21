package com.nextfit.service.churn;

import com.nextfit.exceptions.InvalidDataException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Adds risk when a member who used to come often now comes much less. It compares
 * the visits per week of the last weeks with the weeks before them. A member who
 * barely came before has nothing to lose, and a small drop is normal, so neither
 * adds points.
 */
public class FrequencyDropRule implements ChurnRule {

    private static final double MIN_BASELINE_PER_WEEK = 1.0;
    private static final double MIN_DROP = 0.25;

    private final int maxPoints;
    private final int recentWeeks;
    private final int baselineWeeks;

    /** Creates the rule with 40 points, the last 2 weeks against the 4 before them. */
    public FrequencyDropRule() {
        this(40, 2, 4);
    }

    /**
     * Creates the rule.
     *
     * @param maxPoints     the most points the rule can add
     * @param recentWeeks   how many of the latest weeks are compared
     * @param baselineWeeks how many weeks before them are used as the usual habit
     * @throws InvalidDataException when a number of weeks is not greater than zero
     */
    public FrequencyDropRule(int maxPoints, int recentWeeks, int baselineWeeks) {
        if (recentWeeks <= 0 || baselineWeeks <= 0) {
            throw new InvalidDataException("semanas", "debe ser mayor que cero");
        }
        this.maxPoints = maxPoints;
        this.recentWeeks = recentWeeks;
        this.baselineWeeks = baselineWeeks;
    }

    @Override
    public Optional<ChurnFactor> assess(MemberActivity activity) {
        LocalDate recentFrom = activity.today().minusDays(recentWeeks * 7L - 1);
        LocalDate baselineTo = recentFrom.minusDays(1);
        LocalDate baselineFrom = baselineTo.minusDays(baselineWeeks * 7L - 1);

        double recent = activity.visitsBetween(recentFrom, activity.today()) / (double) recentWeeks;
        double before = activity.visitsBetween(baselineFrom, baselineTo) / (double) baselineWeeks;
        if (before < MIN_BASELINE_PER_WEEK) {
            return Optional.empty();
        }
        double drop = 1 - recent / before;
        if (drop < MIN_DROP) {
            return Optional.empty();
        }
        int points = (int) Math.round(maxPoints * Math.min(1.0, drop));
        return Optional.of(new ChurnFactor(points,
                "Pasó de " + perWeek(before) + " a " + perWeek(recent) + " visitas por semana"));
    }

    // Whole numbers without decimals ("4") and the rest with one decimal and a comma ("0,5").
    private static String perWeek(double visits) {
        if (visits == Math.rint(visits)) {
            return String.valueOf((long) visits);
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f", visits);
    }
}