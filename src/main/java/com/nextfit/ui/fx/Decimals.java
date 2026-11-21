package com.nextfit.ui.fx;

import com.nextfit.exceptions.InvalidDataException;

import java.util.Locale;

/**
 * Reads and writes the decimal numbers typed in the forms of the member file
 * (weight, height, body measures). It accepts a comma or a dot as separator.
 */
final class Decimals {

    private Decimals() {
    }

    /**
     * Reads a decimal number typed by the user.
     *
     * @param field the name of the field, used in the error message
     * @param text  what the user typed
     * @return the number
     * @throws InvalidDataException when the text is empty or is not a number
     */
    static double parse(String field, String text) {
        String cleaned = text == null ? "" : text.trim().replace(',', '.');
        if (cleaned.isEmpty()) {
            throw new InvalidDataException(field, "no puede estar vacío");
        }
        double value;
        try {
            value = Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(field, "debe ser un número, por ejemplo 72.5");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new InvalidDataException(field, "debe ser un número, por ejemplo 72.5");
        }
        return value;
    }

    /**
     * Writes a number with one decimal and a dot, the way the screens show it.
     *
     * @param value the number
     * @return the text, e.g. "72.5"
     */
    static String text(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
