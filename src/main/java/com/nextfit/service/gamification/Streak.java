package com.nextfit.service.gamification;

/**
 * How many weeks in a row a member has met the weekly goal of training days.
 *
 * @param current the weeks in a row that are still going, counted back from the current week
 * @param best    the longest run of weeks in a row the member has ever had
 */
public record Streak(int current, int best) {

    /** A member who has never met the weekly goal. */
    public static final Streak NONE = new Streak(0, 0);
}
