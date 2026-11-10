package com.nextfit.ui.fx;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecimalsTest {

    @Test
    void parseAcceptsDotAndComma() {
        assertEquals(72.5, Decimals.parse("peso", "72.5"), 1e-9);
        assertEquals(72.5, Decimals.parse("peso", "72,5"), 1e-9);
        assertEquals(175.0, Decimals.parse("estatura", "  175 "), 1e-9);
    }

    @Test
    void parseRejectsEmptyOrNotNumbers() {
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", ""));
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", "   "));
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", null));
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", "setenta"));
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", "72,5kg"));
    }

    @Test
    void parseRejectsNanAndInfinity() {
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", "NaN"));
        assertThrows(InvalidDataException.class, () -> Decimals.parse("peso", "Infinity"));
    }

    @Test
    void errorNamesTheField() {
        InvalidDataException error = assertThrows(InvalidDataException.class, () -> Decimals.parse("cintura", "x"));
        assertEquals("cintura", error.getFieldName());
    }

    @Test
    void textUsesOneDecimalAndADot() {
        assertEquals("72.5", Decimals.text(72.54));
        assertEquals("80.0", Decimals.text(80));
        assertEquals("23.0", Decimals.text(22.96));
    }
}
