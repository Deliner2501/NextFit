package com.nextfit.service.face;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceStabilizerTest {

    /** A clock the test moves by hand. */
    private static final class Manual extends Clock {
        private Instant now = Instant.parse("2026-11-12T15:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final Manual clock = new Manual();
    private final FaceStabilizer stabilizer = new FaceStabilizer(3, Duration.ofSeconds(8), clock);

    @Test
    void confirmsAfterThreeFramesInARow() {
        assertTrue(stabilizer.feed(Optional.of(5L)).isEmpty());
        assertTrue(stabilizer.feed(Optional.of(5L)).isEmpty());
        assertEquals(Optional.of(5L), stabilizer.feed(Optional.of(5L)));
    }

    @Test
    void aBadFrameRestartsTheCount() {
        stabilizer.feed(Optional.of(5L));
        stabilizer.feed(Optional.of(5L));
        stabilizer.feed(Optional.empty());
        assertTrue(stabilizer.feed(Optional.of(5L)).isEmpty());
        assertTrue(stabilizer.feed(Optional.of(5L)).isEmpty());
        assertEquals(Optional.of(5L), stabilizer.feed(Optional.of(5L)));
    }

    @Test
    void changingOfMemberRestartsTheCount() {
        stabilizer.feed(Optional.of(5L));
        stabilizer.feed(Optional.of(5L));
        assertTrue(stabilizer.feed(Optional.of(6L)).isEmpty());
        assertTrue(stabilizer.feed(Optional.of(6L)).isEmpty());
        assertEquals(Optional.of(6L), stabilizer.feed(Optional.of(6L)));
    }

    @Test
    void ignoresTheMemberDuringTheCooldownAndAcceptsAgainAfterwards() {
        for (int i = 0; i < 3; i++) {
            stabilizer.feed(Optional.of(5L));
        }
        for (int i = 0; i < 10; i++) {
            assertTrue(stabilizer.feed(Optional.of(5L)).isEmpty());
        }
        clock.advance(Duration.ofSeconds(9));
        stabilizer.feed(Optional.of(5L));
        stabilizer.feed(Optional.of(5L));
        assertEquals(Optional.of(5L), stabilizer.feed(Optional.of(5L)));
    }

    @Test
    void anotherMemberIsNotBlockedByTheCooldownOfThePrevious() {
        for (int i = 0; i < 3; i++) {
            stabilizer.feed(Optional.of(5L));
        }
        stabilizer.feed(Optional.of(6L));
        stabilizer.feed(Optional.of(6L));
        assertEquals(Optional.of(6L), stabilizer.feed(Optional.of(6L)));
    }
}
