package com.nextfit.exceptions;

/**
 * Thrown when an error occurs while reading from or writing to the
 * system's storage (data files now, database later), such as a
 * corrupted file, denied permission, or a full disk. It keeps the
 * original cause to preserve traceability.
 */
public class PersistenceException extends NextFitException {

    private static final String ERROR_CODE = "PERSISTENCE_ERROR";

    /**
     * Creates the exception with a message and the original cause.
     *
     * @param message the description of the problem, including the file
     *                and the operation (read or write) when available
     * @param cause   the original exception (e.g. an IOException)
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, ERROR_CODE);
        initCause(cause);
    }
}
