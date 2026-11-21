package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserValidatorTest {

    private final UserValidator validator = new UserValidator();

    private String badField(String user, String name, String password, Role role) {
        return assertThrows(InvalidDataException.class,
                () -> validator.validateNewUser(user, name, password, role)).getFieldName();
    }

    @Test
    void aCompleteUserIsAccepted() {
        assertDoesNotThrow(() -> validator.validateNewUser("ana.perez", "Ana Pérez", "Clave2026", Role.RECEPTION));
    }

    @Test
    void theUsernameHasLimitsAndAllowedCharacters() {
        assertEquals("usuario", badField("", "Ana", "Clave2026", Role.ADMIN));
        assertThrows(InvalidDataException.class, () -> validator.validateUsername("ab"));
        assertDoesNotThrow(() -> validator.validateUsername("abc"));
        assertDoesNotThrow(() -> validator.validateUsername("a".repeat(20)));
        assertThrows(InvalidDataException.class, () -> validator.validateUsername("a".repeat(21)));
        assertDoesNotThrow(() -> validator.validateUsername("ana_perez.2"));
        assertThrows(InvalidDataException.class, () -> validator.validateUsername("ana perez"));
        assertThrows(InvalidDataException.class, () -> validator.validateUsername("ana-perez"));
        assertThrows(InvalidDataException.class, () -> validator.validateUsername("Andrés"));
    }

    @Test
    void theFullNameIsRequiredAndLimited() {
        assertEquals("nombre completo", badField("ana", " ", "Clave2026", Role.ADMIN));
        assertDoesNotThrow(() -> validator.validateFullName("a".repeat(60)));
        assertThrows(InvalidDataException.class, () -> validator.validateFullName("a".repeat(61)));
    }

    @Test
    void thePasswordNeedsEightCharactersWithLettersAndDigits() {
        assertEquals("contraseña", badField("ana", "Ana", "Ab1", Role.ADMIN));
        assertThrows(InvalidDataException.class, () -> validator.validatePassword(null));
        assertThrows(InvalidDataException.class, () -> validator.validatePassword("12345678"));
        assertThrows(InvalidDataException.class, () -> validator.validatePassword("abcdefgh"));
        assertDoesNotThrow(() -> validator.validatePassword("abcdefg1"));
    }

    @Test
    void theRoleIsRequired() {
        assertEquals("rol", badField("ana", "Ana", "Clave2026", null));
    }
}
