package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.TrainingSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainingSessionValidatorTest {

    private final TrainingSessionValidator validator = new TrainingSessionValidator();

    private static TrainingSession session(long member, long assignment, String day, LocalDate date, int minutes,
                                           String notes) {
        return new TrainingSession(0, member, assignment, day, date, minutes, notes);
    }

    private String badField(TrainingSession session) {
        return assertThrows(InvalidDataException.class, () -> validator.validate(session)).getFieldName();
    }

    @Test
    void aCompleteSessionIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(session(1, 1, "Espalda y bíceps", LocalDate.now(), 60, "Buen ritmo")));
    }

    @Test
    void theMemberAndTheAssignmentAreRequired() {
        assertEquals("socio", badField(session(0, 1, "Día 1", LocalDate.now(), 60, "")));
        assertEquals("asignación", badField(session(1, 0, "Día 1", LocalDate.now(), 60, "")));
    }

    @Test
    void theDayNameIsRequiredAndLimited() {
        assertEquals("día de la rutina", badField(session(1, 1, " ", LocalDate.now(), 60, "")));
        assertDoesNotThrow(() -> validator.validateDayName("a".repeat(60)));
        assertThrows(InvalidDataException.class, () -> validator.validateDayName("a".repeat(61)));
    }

    @Test
    void theDateIsRequiredAndCannotBeInTheFuture() {
        assertEquals("fecha", badField(session(1, 1, "Día 1", null, 60, "")));
        assertEquals("fecha", badField(session(1, 1, "Día 1", LocalDate.now().plusDays(1), 60, "")));
    }

    @Test
    void theDurationIsBetweenFiveAndThreeHundredMinutes() {
        assertThrows(InvalidDataException.class, () -> validator.validateDuration(4));
        assertDoesNotThrow(() -> validator.validateDuration(5));
        assertDoesNotThrow(() -> validator.validateDuration(300));
        assertThrows(InvalidDataException.class, () -> validator.validateDuration(301));
    }

    @Test
    void theNotesAreOptionalAndLimited() {
        assertDoesNotThrow(() -> validator.validateNotes(null));
        assertDoesNotThrow(() -> validator.validateNotes("x".repeat(200)));
        assertThrows(InvalidDataException.class, () -> validator.validateNotes("x".repeat(201)));
    }
}
