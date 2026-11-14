package com.nextfit.service.face;

/** The camera or the recognition models could not be used. The message is ready to show to the user. */
public class FaceCameraException extends RuntimeException {

    public FaceCameraException(String message) {
        super(message);
    }

    public FaceCameraException(String message, Throwable cause) {
        super(message, cause);
    }
}
