package com.nextfit.exceptions;

/**
 * Raised when the artificial intelligence service cannot answer: there is no key, there
 * is no internet, the service refused the request, or its answer could not be read. The
 * message is written in Spanish so it can be shown to the user as it is.
 */
public class AiException extends NextFitException {

    private static final String ERROR_CODE = "AI_ERROR";

    /**
     * @param message the explanation for the user, in Spanish
     */
    public AiException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * @param message the explanation for the user, in Spanish
     * @param cause   the technical cause
     */
    public AiException(String message, Throwable cause) {
        super(message, ERROR_CODE);
        initCause(cause);
    }
}