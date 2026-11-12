package com.nextfit.service.portal;

import com.nextfit.model.Member;
import com.nextfit.service.gamification.Achievements;

import java.util.Optional;

/**
 * Everything the member sees in the phone portal. It is a plain value: it does not know how it
 * will be drawn.
 *
 * @param member       the member
 * @param membership   the valid membership, or empty when there is none today
 * @param workout      the day of the routine to train next, or empty when there is no routine
 * @param achievements the training days, the streaks and the badges
 */
public record PortalSummary(Member member, Optional<PortalMembership> membership,
                            Optional<PortalWorkout> workout, Achievements achievements) {
}
