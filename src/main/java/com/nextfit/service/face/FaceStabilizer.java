package com.nextfit.service.face;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Avoids mistakes caused by a single bad frame: a member is confirmed only after being recognized in
 * several frames in a row, and once confirmed it is ignored for a while, so standing in front of the
 * camera does not enter and leave again and again.
 */
public final class FaceStabilizer {

    private final int needed;
    private final Duration cooldown;
    private final Clock clock;
    private final Map<Long, Instant> pausedUntil = new HashMap<>();
    private Long candidate;
    private int streak;

    /**
     * @param needed   consecutive frames with the same member to confirm it
     * @param cooldown time during which a confirmed member is ignored
     * @param clock    the clock
     */
    public FaceStabilizer(int needed, Duration cooldown, Clock clock) {
        this.needed = needed;
        this.cooldown = cooldown;
        this.clock = clock;
    }

    /**
     * Gives the result of one frame.
     *
     * @param recognized the member recognized in the frame, or empty
     * @return the member when it was just confirmed, otherwise empty
     */
    public synchronized Optional<Long> feed(Optional<Long> recognized) {
        if (recognized.isEmpty()) {
            candidate = null;
            streak = 0;
            return Optional.empty();
        }
        long id = recognized.get();
        Instant now = clock.instant();
        Instant until = pausedUntil.get(id);
        if (until != null && now.isBefore(until)) {
            return Optional.empty();
        }
        if (candidate != null && candidate == id) {
            streak++;
        } else {
            candidate = id;
            streak = 1;
        }
        if (streak >= needed) {
            pausedUntil.put(id, now.plus(cooldown));
            candidate = null;
            streak = 0;
            return Optional.of(id);
        }
        return Optional.empty();
    }
}
