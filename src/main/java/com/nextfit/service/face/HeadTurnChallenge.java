package com.nextfit.service.face;

/**
 * A simple proof that a real person is in front of the camera: the person must turn the head to both
 * sides. A still photo never changes its angle, so it never passes. A recorded video could, which is a
 * known limit of this check.
 */
public final class HeadTurnChallenge {

    private final double threshold;
    private boolean sawLeft;
    private boolean sawRight;

    /** @param threshold how far from the center the head must turn (0.2 is a gentle turn) */
    public HeadTurnChallenge(double threshold) {
        this.threshold = threshold;
    }

    /**
     * Gives the head angle of one frame.
     *
     * @param yaw the angle of the head: 0 looking straight, negative to one side, positive to the other
     * @return true when the head has already turned to both sides
     */
    public boolean feed(double yaw) {
        if (yaw <= -threshold) {
            sawLeft = true;
        } else if (yaw >= threshold) {
            sawRight = true;
        }
        return passed();
    }

    /** @return true when the head has turned to both sides */
    public boolean passed() {
        return sawLeft && sawRight;
    }

    /** Starts the challenge again. */
    public void reset() {
        sawLeft = false;
        sawRight = false;
    }
}
