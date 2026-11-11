package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.MembershipPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MembershipPlanValidatorTest {

    private final MembershipPlanValidator validator = new MembershipPlanValidator();

    private String badField(MembershipPlan plan) {
        return assertThrows(InvalidDataException.class, () -> validator.validate(plan)).getFieldName();
    }

    @Test
    void aCompletePlanIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(new MembershipPlan(0, "Mensual", "Acceso total", 30, 80_000, true)));
    }

    @Test
    void theNameIsRequiredAndBetweenThreeAndFortyCharacters() {
        assertEquals("nombre", badField(new MembershipPlan(0, " ", "", 30, 80_000, true)));
        assertThrows(InvalidDataException.class, () -> validator.validateName("ab"));
        assertDoesNotThrow(() -> validator.validateName("abc"));
        assertDoesNotThrow(() -> validator.validateName("a".repeat(40)));
        assertThrows(InvalidDataException.class, () -> validator.validateName("a".repeat(41)));
        assertThrows(InvalidDataException.class, () -> validator.validateName(null));
    }

    @Test
    void theDescriptionIsOptionalAndLimited() {
        assertDoesNotThrow(() -> validator.validateDescription(null));
        assertDoesNotThrow(() -> validator.validateDescription("x".repeat(120)));
        assertThrows(InvalidDataException.class, () -> validator.validateDescription("x".repeat(121)));
    }

    @Test
    void theDurationIsBetweenOneAndSevenHundredThirtyDays() {
        assertEquals("duración", badField(new MembershipPlan(0, "Mensual", "", 0, 80_000, true)));
        assertDoesNotThrow(() -> validator.validateDuration(1));
        assertDoesNotThrow(() -> validator.validateDuration(730));
        assertThrows(InvalidDataException.class, () -> validator.validateDuration(731));
        assertThrows(InvalidDataException.class, () -> validator.validateDuration(-5));
    }

    @Test
    void thePriceMustBePositiveAndNotAbsurd() {
        assertEquals("precio", badField(new MembershipPlan(0, "Mensual", "", 30, 0, true)));
        assertDoesNotThrow(() -> validator.validatePrice(1));
        assertDoesNotThrow(() -> validator.validatePrice(50_000_000L));
        assertThrows(InvalidDataException.class, () -> validator.validatePrice(50_000_001L));
        assertThrows(InvalidDataException.class, () -> validator.validatePrice(-1));
    }
}
