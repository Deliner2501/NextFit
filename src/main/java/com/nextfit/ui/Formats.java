package com.nextfit.ui;

import com.nextfit.exceptions.InvalidDataException;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Turns values into the text the screens show (pesos, dates) and reads what
 * the user types back into numbers. Keeping this in one place makes every
 * screen write money and dates the same way.
 */
public final class Formats {

    private static final String[] MONTHS = {"ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"};

    private Formats() {
    }

    /**
     * Writes an amount of pesos with a dot between thousands.
     *
     * @param pesos the amount
     * @return the text, e.g. "$ 80.000"
     */
    public static String money(long pesos) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        String digits = new DecimalFormat("#,##0", symbols).format(Math.abs(pesos));
        return (pesos < 0 ? "-$ " : "$ ") + digits;
    }

    /**
     * Writes a day with the month abbreviated in Spanish.
     *
     * @param day the day
     * @return the text, e.g. "27 oct 2026"
     */
    public static String date(LocalDate day) {
        return day.getDayOfMonth() + " " + MONTHS[day.getMonthValue() - 1] + " " + day.getYear();
    }

    /**
     * Writes a day and a time of day.
     *
     * @param moment the moment
     * @return the text, e.g. "27 oct 2026 15:45"
     */
    public static String dateTime(LocalDateTime moment) {
        return date(moment.toLocalDate()) + String.format(" %02d:%02d", moment.getHour(), moment.getMinute());
    }

    /**
     * Writes a month with its abbreviation, to label a chart.
     *
     * @param month the month
     * @return the text, e.g. "oct"
     */
    public static String monthShort(YearMonth month) {
        return MONTHS[month.getMonthValue() - 1];
    }

    /**
     * Reads an amount of pesos typed by the user. It accepts "80000",
     * "80.000", "$ 80.000" and spaces.
     *
     * @param field the name of the field, used in the error message
     * @param text  what the user typed
     * @return the amount
     * @throws InvalidDataException when the text is empty or is not a whole number
     */
    public static long parseMoney(String field, String text) {
        String cleaned = text == null ? "" : text.replace("$", "").replace(".", "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            throw new InvalidDataException(field, "no puede estar vacío");
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(field, "debe ser un número entero, por ejemplo 80000");
        }
    }

    /**
     * Reads a whole number typed by the user.
     *
     * @param field the name of the field, used in the error message
     * @param text  what the user typed
     * @return the number
     * @throws InvalidDataException when the text is empty or is not a whole number
     */
    public static int parseWholeNumber(String field, String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            throw new InvalidDataException(field, "no puede estar vacío");
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            throw new InvalidDataException(field, "debe ser un número entero");
        }
    }

    /**
     * Reads a day typed as year-month-day.
     *
     * @param field the name of the field, used in the error message
     * @param text  what the user typed
     * @return the day, or null when the text is empty
     * @throws InvalidDataException when the text is not a valid day
     */
    public static LocalDate parseDate(String field, String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(cleaned);
        } catch (java.time.format.DateTimeParseException e) {
            throw new InvalidDataException(field, "escríbela como AAAA-MM-DD, por ejemplo 2026-10-27");
        }
    }
}
