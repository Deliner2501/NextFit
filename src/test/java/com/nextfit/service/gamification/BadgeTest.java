package com.nextfit.service.gamification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BadgeTest {

    @Test
    void daysBadgesNeedTheirNumberOfDays() {
        assertFalse(Badge.FIRST_STEP.isEarnedBy(0, 0));
        assertTrue(Badge.FIRST_STEP.isEarnedBy(1, 0));
        assertFalse(Badge.STEADY.isEarnedBy(9, 0));
        assertTrue(Badge.STEADY.isEarnedBy(10, 0));
        assertFalse(Badge.UNSTOPPABLE.isEarnedBy(49, 0));
        assertTrue(Badge.UNSTOPPABLE.isEarnedBy(50, 0));
        assertFalse(Badge.LEGEND.isEarnedBy(99, 0));
        assertTrue(Badge.LEGEND.isEarnedBy(100, 0));
    }

    @Test
    void streakBadgesNeedTheirNumberOfWeeks() {
        assertFalse(Badge.MONTH_STREAK.isEarnedBy(500, 3));
        assertTrue(Badge.MONTH_STREAK.isEarnedBy(0, 4));
        assertFalse(Badge.QUARTER_STREAK.isEarnedBy(500, 11));
        assertTrue(Badge.QUARTER_STREAK.isEarnedBy(0, 12));
    }

    @Test
    void everyBadgeHasAnameAndADescription() {
        for (Badge badge : Badge.values()) {
            assertFalse(badge.getLabel().isBlank());
            assertFalse(badge.getDescription().isBlank());
        }
    }
}
