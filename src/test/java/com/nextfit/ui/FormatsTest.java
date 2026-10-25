package com.nextfit.ui;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatsTest {

    @Test
    void moneyUsesDotsBetweenThousands() {
        assertEquals("$ 0", Formats.money(0));
        assertEquals("$ 950", Formats.money(950));
        assertEquals("$ 80.000", Formats.money(80_000));
        assertEquals("$ 6.840.000", Formats.money(6_840_000));
    }

    @Test
    void moneyKeepsTheSignOfNegativeAmounts() {
        assertEquals("-$ 1.500", Formats.money(-1_500));
    }

    @Test
    void datesUseSpanishMonthAbbreviations() {
        assertEquals("27 oct 2026", Formats.date(LocalDate.of(2026, 10, 27)));
        assertEquals("1 ene 2027", Formats.date(LocalDate.of(2027, 1, 1)));
        assertEquals("27 oct 2026 09:05", Formats.dateTime(LocalDateTime.of(2026, 10, 27, 9, 5)));
        assertEquals("dic", Formats.monthShort(YearMonth.of(2026, 12)));
    }

    @Test
    void parseMoneyAcceptsCommonWaysOfTypingAnAmount() {
        assertEquals(80_000, Formats.parseMoney("precio", "80000"));
        assertEquals(80_000, Formats.parseMoney("precio", "80.000"));
        assertEquals(80_000, Formats.parseMoney("precio", " $ 80.000 "));
    }

    @Test
    void parseMoneyRejectsEmptyOrNonNumericText() {
        assertThrows(InvalidDataException.class, () -> Formats.parseMoney("precio", ""));
        assertThrows(InvalidDataException.class, () -> Formats.parseMoney("precio", "   "));
        assertThrows(InvalidDataException.class, () -> Formats.parseMoney("precio", "ochenta mil"));
        assertThrows(InvalidDataException.class, () -> Formats.parseMoney("precio", null));
    }

    @Test
    void parseWholeNumberRejectsDecimalsAndEmptyText() {
        assertEquals(30, Formats.parseWholeNumber("duración", " 30 "));
        assertThrows(InvalidDataException.class, () -> Formats.parseWholeNumber("duración", "30.5"));
        assertThrows(InvalidDataException.class, () -> Formats.parseWholeNumber("duración", ""));
    }

    @Test
    void parseDateReadsIsoDatesAndTreatsEmptyTextAsNoDate() {
        assertEquals(LocalDate.of(2026, 10, 27), Formats.parseDate("inicio", "2026-10-27"));
        assertNull(Formats.parseDate("inicio", "  "));
        assertThrows(InvalidDataException.class, () -> Formats.parseDate("inicio", "27/10/2026"));
    }
}
