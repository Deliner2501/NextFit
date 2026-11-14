package com.nextfit.service.face;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the pictures of a face for registering a member. It keeps only clear frontal faces and waits
 * a moment between pictures so they are not three copies of the same instant.
 */
public final class FaceSampleCollector {

    public static final int REQUIRED = 3;
    /** Stricter than the entry check: a registered face must look straight at the camera. */
    public static final double ENROLL_YAW = 0.2;
    public static final Duration SPACING = Duration.ofMillis(900);

    private final Clock clock;
    private final List<float[]> samples = new ArrayList<>();
    private Instant lastTaken = Instant.MIN;

    /** @param clock the clock */
    public FaceSampleCollector(Clock clock) {
        this.clock = clock;
    }

    /**
     * Offers one frame.
     *
     * @param frame what the camera saw
     * @return true when this frame was kept as a sample
     */
    public boolean offer(FaceFrame frame) {
        if (complete() || !frame.hasFace() || Math.abs(frame.yaw()) > ENROLL_YAW) {
            return false;
        }
        Instant now = clock.instant();
        if (now.isBefore(lastTaken.plus(SPACING))) {
            return false;
        }
        samples.add(frame.vector());
        lastTaken = now;
        return true;
    }

    /** @return how many samples were kept */
    public int count() {
        return samples.size();
    }

    /** @return true when there are enough samples */
    public boolean complete() {
        return samples.size() >= REQUIRED;
    }

    /** @return the kept samples */
    public List<float[]> samples() {
        return List.copyOf(samples);
    }

    /** @return what to tell the person about the current frame */
    public String hint(FaceFrame frame) {
        if (!frame.hasFace()) {
            return "No veo ningún rostro. Colócate frente a la cámara.";
        }
        if (Math.abs(frame.yaw()) > ENROLL_YAW) {
            return "Mira de frente a la cámara.";
        }
        return "Quédate quieto... foto " + Math.min(count() + 1, REQUIRED) + " de " + REQUIRED;
    }
}
