package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyTokenBudgetTest {

    private MovableClock clock;
    private DailyTokenBudget budget;

    @BeforeEach
    void setUp() {
        clock = new MovableClock(Instant.parse("2026-11-13T15:00:00Z"));
        budget = new DailyTokenBudget(1000, clock);
    }

    @Test
    void thereIsRoomWhileTheLimitIsNotReached() {
        budget.record(300, 200);

        assertDoesNotThrow(budget::ensureAvailable);
        assertEquals(500, budget.usedToday());
    }

    @Test
    void whenTheLimitIsReachedItStops() {
        budget.record(600, 400);

        assertThrows(AiException.class, budget::ensureAvailable);
    }

    @Test
    void theCountStartsAgainTomorrow() {
        budget.record(600, 400);
        clock.advance(Duration.ofDays(1));

        assertDoesNotThrow(budget::ensureAvailable);
        assertEquals(0, budget.usedToday());
    }

    @Test
    void theLimitMustBeGreaterThanZero() {
        assertThrows(InvalidDataException.class, () -> new DailyTokenBudget(0, clock));
    }

    /** A clock that only moves when the test says so. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration time) {
            now = now.plus(time);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}