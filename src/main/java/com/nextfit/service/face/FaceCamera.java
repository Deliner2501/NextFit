package com.nextfit.service.face;

/** A camera that finds the face in front of it and describes it with numbers. */
public interface FaceCamera extends AutoCloseable {

    /**
     * Takes the next picture.
     *
     * @return what the camera saw
     * @throws FaceCameraException when the camera cannot be read
     */
    FaceFrame next();

    /** Turns the camera off. */
    @Override
    void close();
}
