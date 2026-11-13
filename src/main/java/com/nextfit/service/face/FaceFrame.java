package com.nextfit.service.face;

/**
 * What the camera saw in one moment.
 *
 * @param preview the image to show on screen (JPEG), with a box around the face when there is one
 * @param vector  the numbers that describe the largest face, or null when there is no face
 * @param yaw     the angle of the head, 0 looking straight (only meaningful when there is a face)
 */
public record FaceFrame(byte[] preview, float[] vector, double yaw) {

    /** Largest head angle (see {@code yaw}) at which a face is still looked at "from the front". */
    public static final double FRONTAL_YAW = 0.35;

    /** @return true when the face looks at the camera, not to the side, so it can be recognized reliably */
    public boolean isFrontal() {
        return hasFace() && Math.abs(yaw) <= FRONTAL_YAW;
    }

    /** @return true when a face was found in the image */
    public boolean hasFace() {
        return vector != null;
    }
}
