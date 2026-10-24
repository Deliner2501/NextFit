package com.nextfit.exceptions;

/**
 * Abstract root of the NextFit exception hierarchy. Every custom
 * exception of the system extends this class, directly or indirectly.
 * It is unchecked (a RuntimeException) so that service and repository
 * method signatures do not need to declare it, while still allowing
 * the user interface to catch each category separately.
 */
public abstract class NextFitException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "GENERAL_ERROR";

    private final String errorCode;

    /**
     * Creates an exception with a message and the default error code.
     *
     * @param message the description of the problem
     */
    public NextFitException(String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    /**
     * Creates an exception with a message and a specific error code.
     *
     * @param message   the description of the problem
     * @param errorCode the code identifying the category of the error
     */
    public NextFitException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code that identifies the category of this error.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}