package com.nextfit.service.gamification;

/**
 * The badges a member can earn. Each one is earned by reaching a number of training days or
 * a streak of weeks. To add a badge it is enough to add a constant here; the service does
 * not change.
 */
public enum Badge {

    /** The first day at the gym. */
    FIRST_STEP("Primer paso", "Entrenó por primera vez", Goal.DAYS, 1),
    /** Ten different training days. */
    STEADY("Constante", "Entrenó 10 días distintos", Goal.DAYS, 10),
    /** Fifty different training days. */
    UNSTOPPABLE("Imparable", "Entrenó 50 días distintos", Goal.DAYS, 50),
    /** One hundred different training days. */
    LEGEND("Leyenda", "Entrenó 100 días distintos", Goal.DAYS, 100),
    /** Four weeks in a row meeting the weekly goal. */
    MONTH_STREAK("Racha de un mes", "4 semanas seguidas cumpliendo la meta semanal", Goal.STREAK, 4),
    /** Twelve weeks in a row meeting the weekly goal. */
    QUARTER_STREAK("Racha de un trimestre", "12 semanas seguidas cumpliendo la meta semanal", Goal.STREAK, 12);

    /** What a badge asks for. */
    private enum Goal {
        /** A number of different training days. */
        DAYS,
        /** A number of weeks in a row. */
        STREAK
    }

    private final String label;
    private final String description;
    private final Goal goal;
    private final int target;

    Badge(String label, String description, Goal goal, int target) {
        this.label = label;
        this.description = description;
        this.goal = goal;
        this.target = target;
    }

    /**
     * Returns the short name shown on screen.
     *
     * @return the name of the badge
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns what the member did to earn it.
     *
     * @return the description of the badge
     */
    public String getDescription() {
        return description;
    }

    /**
     * Tells whether a member with these numbers has earned the badge.
     *
     * @param trainingDays the different days the member has trained
     * @param bestStreak   the longest streak of weeks the member has had; a badge is never lost
     * @return true when the member reached the goal of this badge
     */
    public boolean isEarnedBy(int trainingDays, int bestStreak) {
        return (goal == Goal.DAYS ? trainingDays : bestStreak) >= target;
    }
}
