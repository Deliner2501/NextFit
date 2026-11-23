package com.nextfit.ui.fx;

import com.nextfit.exceptions.InvalidDataException;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Allows a birth date to be typed directly without navigating through decades. */
final class BirthDateField extends DatePicker {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    BirthDateField() {
        setEditable(true);
        setPromptText("DD/MM/AAAA");
        setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(FORMAT);
            }

            @Override
            public LocalDate fromString(String text) {
                try {
                    return parse(text);
                } catch (DateTimeParseException invalid) {
                    // Keep the typed value in the editor for validation when the form is submitted.
                    return getValue();
                }
            }
        });
    }

    /** @param date the member's existing birth date */
    void setDate(LocalDate date) {
        setValue(date);
    }

    /** @return the date currently typed in the editor */
    LocalDate readDate() {
        try {
            return parse(getEditor().getText());
        } catch (DateTimeParseException failure) {
            throw new InvalidDataException("fecha de nacimiento", "usa una fecha válida en formato DD/MM/AAAA");
        }
    }

    private static LocalDate parse(String text) {
        return text == null || text.isBlank() ? null : LocalDate.parse(text.strip(), FORMAT);
    }
}
