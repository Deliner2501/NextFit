package com.nextfit.service.security;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InactivityMonitorTest {

    private MovableClock clock;
    private InactivityMonitor monitor;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        monitor = new InactivityMonitor(15, clock);
    }

    @Test
    void isNotExpiredAtTheStart() {
        assertFalse(monitor.isExpired());
    }

    @Test
    void isNotExpiredJustBeforeTheLimit() {
        clock.advance(Duration.ofMinutes(14).plusSeconds(59));

        assertFalse(monitor.isExpired());
    }

    @Test
    void expiresWhenTheLimitPasses() {
        clock.advance(Duration.ofMinutes(15));

        assertTrue(monitor.isExpired());
    }

    @Test
    void touchRestartsTheCount() {
        clock.advance(Duration.ofMinutes(10));
        monitor.touch();
        clock.advance(Duration.ofMinutes(10));

        assertFalse(monitor.isExpired());
    }

    @Test
    void touchDoesNotRescueAnAlreadyLongInactivityBeforeIt() {
        clock.advance(Duration.ofMinutes(20));
        assertTrue(monitor.isExpired());

        monitor.touch();

        assertFalse(monitor.isExpired());
    }

    @Test
    void theLimitMustBeGreaterThanZero() {
        assertThrows(InvalidDataException.class, () -> new InactivityMonitor(0, clock));
    }

    // Un reloj que se puede adelantar, para probar el paso del tiempo sin esperar.
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-07T15:00:00Z");

        void advance(Duration time) {
            now = now.plus(time);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Bogota");
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