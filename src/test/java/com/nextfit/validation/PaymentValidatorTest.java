package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentValidatorTest {

    private final PaymentValidator validator = new PaymentValidator();

    @Test
    void aSaleWithMethodAndUserIsAccepted() {
        for (PaymentMethod method : PaymentMethod.values()) {
            assertDoesNotThrow(() -> validator.validateSale(method, "recepcion"));
        }
    }

    @Test
    void theMethodIsRequired() {
        assertEquals("método de pago",
                assertThrows(InvalidDataException.class, () -> validator.validateSale(null, "recepcion")).getFieldName());
    }

    @Test
    void whoRegisteredTheSaleIsRequired() {
        assertEquals("registrado por",
                assertThrows(InvalidDataException.class, () -> validator.validateSale(PaymentMethod.CASH, " ")).getFieldName());
        assertThrows(InvalidDataException.class, () -> validator.validateSale(PaymentMethod.CASH, null));
    }
}
