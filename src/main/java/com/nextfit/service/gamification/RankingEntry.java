package com.nextfit.service.gamification;

import com.nextfit.model.Member;

/**
 * A line of the monthly ranking. Members with the same number of days share the position.
 *
 * @param position the place in the ranking, starting at 1
 * @param member   the member
 * @param days     the different days the member trained in the month
 */
public record RankingEntry(int position, Member member, int days) {
}
