package com.nextfit.service.security;

import com.nextfit.exceptions.InvalidDataException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Tells whether the user has been inactive for too long. It only keeps the moment of the last
 * activity and compares it with the clock: it does not know about windows, mouse or keyboard,
 * so the screen decides what counts as activity and when to ask. The clock is injected, so
 * tests can move the time.
 */
public class InactivityMonitor {

    /** Minutes without activity after which the session should be closed. */
    public static final int DEFAULT_MINUTES = 15;

    private final Duration limit;
    private final Clock clock;
    private Instant lastActivity;

    /** Creates the monitor with the default limit and the system clock. */
    public InactivityMonitor() {
        this(DEFAULT_MINUTES, Clock.systemDefaultZone());
    }

    /**
     * Creates the monitor.
     *
     * @param minutes minutes without activity that expire the session, greater than zero
     * @param clock   the source of the current time
     * @throws InvalidDataException when the minutes are not greater than zero
     */
    public InactivityMonitor(int minutes, Clock clock) {
        if (minutes <= 0) {
            throw new InvalidDataException("minutos de inactividad", "deben ser más de cero");
        }
        this.limit = Duration.ofMinutes(minutes);
        this.clock = clock;
        this.lastActivity = clock.instant();
    }

    /** Records that the user did something right now, which restarts the count. */
    public synchronized void touch() {
        lastActivity = clock.instant();
    }

    /**
     * Tells whether the limit has passed since the last activity.
     *
     * @return true when the user has been inactive for the whole limit
     */
    public synchronized boolean isExpired() {
        return !clock.instant().isBefore(lastActivity.plus(limit));
    }
}