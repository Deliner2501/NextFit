package com.nextfit.service.gamification;

import com.nextfit.model.Member;

import java.util.List;

/**
 * Everything the gym knows about the progress of a member: how many days he or she has
 * trained, the streak and the badges earned, in the order of {@link Badge}.
 *
 * @param member       the member
 * @param trainingDays the different days the member has come to the gym
 * @param streak       the current and the best streak of weeks
 * @param badges       the badges earned so far
 */
public record Achievements(Member member, int trainingDays, Streak streak, List<Badge> badges) {
}
