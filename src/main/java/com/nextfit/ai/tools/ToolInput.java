package com.nextfit.ai.tools;

import com.nextfit.exceptions.InvalidDataException;

/**
 * Helpers to read the arguments that the AI sends to a tool. The AI writes them, so they are
 * never trusted: a wrong value becomes a clear error that the AI can read and correct.
 */
final class ToolInput {

    private ToolInput() {
    }

    /**
     * Reads a "limit" argument.
     *
     * @param value        what the AI sent, or null
     * @param defaultLimit the limit when nothing was sent
     * @param maxLimit     the most that is allowed; a bigger number is reduced to it
     * @return a limit between 1 and the maximum
     * @throws InvalidDataException when the value is not a number
     */
    static int limit(Object value, int defaultLimit, int maxLimit) {
        if (value == null) {
            return defaultLimit;
        }
        if (!(value instanceof Number number)) {
            throw new InvalidDataException("límite", "debe ser un número");
        }
        return Math.max(1, Math.min(maxLimit, number.intValue()));
    }
}