package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberValidatorTest {

    private final MemberValidator validator = new MemberValidator();

    private static Member member(String name, String document, String phone, String email, LocalDate birth,
                                 LocalDate join) {
        return new Member(0, name, document, phone, email, birth, join, "", "Mamá 3001112233", true);
    }

    private static Member valid() {
        return member("Ana Pérez", "1065123456", "3001112233", "ana@example.com",
                LocalDate.now().minusYears(25), LocalDate.now().minusDays(10));
    }

    private String badField(Member member) {
        return assertThrows(InvalidDataException.class, () -> validator.validate(member)).getFieldName();
    }

    @Test
    void aCompleteMemberIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(valid()));
    }

    @Test
    void theEmailIsOptionalButMustBeWellFormedWhenGiven() {
        assertDoesNotThrow(() -> validator.validateEmail(""));
        assertDoesNotThrow(() -> validator.validateEmail(null));
        assertThrows(InvalidDataException.class, () -> validator.validateEmail("ana@"));
        assertThrows(InvalidDataException.class, () -> validator.validateEmail("ana example@x.co"));
    }

    @Test
    void theNameIsRequiredAndLimited() {
        assertEquals("nombre completo", badField(member(" ", "1065123456", "3001112233", "",
                LocalDate.now().minusYears(25), LocalDate.now())));
        assertThrows(InvalidDataException.class, () -> validator.validateFullName("a".repeat(61)));
        assertDoesNotThrow(() -> validator.validateFullName("a".repeat(60)));
    }

    @Test
    void theDocumentMustBeFiveToTwelveDigits() {
        assertThrows(InvalidDataException.class, () -> validator.validateDocumentNumber("1234"));
        assertDoesNotThrow(() -> validator.validateDocumentNumber("12345"));
        assertDoesNotThrow(() -> validator.validateDocumentNumber("123456789012"));
        assertThrows(InvalidDataException.class, () -> validator.validateDocumentNumber("1234567890123"));
        assertThrows(InvalidDataException.class, () -> validator.validateDocumentNumber("12a45"));
        assertThrows(InvalidDataException.class, () -> validator.validateDocumentNumber(null));
    }

    @Test
    void thePhoneMustBeSevenToFifteenDigits() {
        assertThrows(InvalidDataException.class, () -> validator.validatePhone("123456"));
        assertDoesNotThrow(() -> validator.validatePhone("1234567"));
        assertDoesNotThrow(() -> validator.validatePhone("123456789012345"));
        assertThrows(InvalidDataException.class, () -> validator.validatePhone("+573001112233"));
    }

    @Test
    void theAgeMustBeBetweenTwelveAndOneHundred() {
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(null));
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(LocalDate.now().plusDays(1)));
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(LocalDate.now().minusYears(11)));
        assertDoesNotThrow(() -> validator.validateBirthDate(LocalDate.now().minusYears(13)));
        assertThrows(InvalidDataException.class, () -> validator.validateBirthDate(LocalDate.now().minusYears(101)));
    }

    @Test
    void theJoinDateCannotBeInTheFuture() {
        assertThrows(InvalidDataException.class, () -> validator.validateJoinDate(null));
        assertThrows(InvalidDataException.class, () -> validator.validateJoinDate(LocalDate.now().plusDays(1)));
        assertDoesNotThrow(() -> validator.validateJoinDate(LocalDate.now()));
    }

    @Test
    void theEmergencyContactIsOptionalAndLimited() {
        assertDoesNotThrow(() -> validator.validateEmergencyContact(null));
        assertDoesNotThrow(() -> validator.validateEmergencyContact("x".repeat(80)));
        assertThrows(InvalidDataException.class, () -> validator.validateEmergencyContact("x".repeat(81)));
    }
}
