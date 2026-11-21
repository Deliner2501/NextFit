package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Trainer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainerValidatorTest {

    private final TrainerValidator validator = new TrainerValidator();

    private static Trainer trainer(String name, String document, String phone, String email, LocalDate birth,
                                   String specialty) {
        return new Trainer(0, name, document, phone, email, birth, specialty, "", true);
    }

    private static Trainer valid() {
        return trainer("Mario Prueba", "1090000111", "3201112233", "mario@nextfit.com",
                LocalDate.now().minusYears(30), "Funcional");
    }

    private String badField(Trainer trainer) {
        return assertThrows(InvalidDataException.class, () -> validator.validate(trainer)).getFieldName();
    }

    @Test
    void aCompleteTrainerIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(valid()));
    }

    @Test
    void eachRequiredFieldIsReportedByName() {
        LocalDate birth = LocalDate.now().minusYears(30);
        assertEquals("nombre completo", badField(trainer(" ", "1090000111", "3201112233", "", birth, "Funcional")));
        assertEquals("documento", badField(trainer("Mario", "12", "3201112233", "", birth, "Funcional")));
        assertEquals("teléfono", badField(trainer("Mario", "1090000111", "abc", "", birth, "Funcional")));
        assertEquals("correo", badField(trainer("Mario", "1090000111", "3201112233", "mario@", birth, "Funcional")));
        assertEquals("fecha de nacimiento", badField(trainer("Mario", "1090000111", "3201112233", "", null, "Funcional")));
        assertEquals("especialidad", badField(trainer("Mario", "1090000111", "3201112233", "", birth, " ")));
    }

    @Test
    void theEmailIsOptional() {
        assertDoesNotThrow(() -> validator.validateEmail(null));
        assertDoesNotThrow(() -> validator.validateEmail(""));
    }

    @Test
    void aTrainerMustBeBetweenEighteenAndEightyYearsOld() {
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(LocalDate.now().minusYears(17)));
        assertDoesNotThrow(() -> validator.validateBirthDate(LocalDate.now().minusYears(18)));
        assertDoesNotThrow(() -> validator.validateBirthDate(LocalDate.now().minusYears(80)));
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(LocalDate.now().minusYears(81)));
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(LocalDate.now().plusDays(1)));
    }

    @Test
    void theSpecialtyHasLimits() {
        assertThrows(InvalidDataException.class, () -> validator.validateSpecialty("ab"));
        assertDoesNotThrow(() -> validator.validateSpecialty("abc"));
        assertDoesNotThrow(() -> validator.validateSpecialty("a".repeat(40)));
        assertThrows(InvalidDataException.class, () -> validator.validateSpecialty("a".repeat(41)));
    }
}
