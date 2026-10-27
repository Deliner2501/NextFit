package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.PaymentMethod;

/**
 * Validates the data needed to register a payment. It has no access to files
 * or screens, so it can be tested on its own.
 */
public class PaymentValidator {

    /**
     * Validates the data that the staff member must provide.
     *
     * @param method       the way the member pays
     * @param registeredBy the user name of the staff member who registers the payment
     * @throws InvalidDataException when the method is missing or nobody is registering
     */
    public void validateSale(PaymentMethod method, String registeredBy) {
        if (method == null) {
            throw new InvalidDataException("método de pago", "debe seleccionar un método de pago");
        }
        if (registeredBy == null || registeredBy.isBlank()) {
            throw new InvalidDataException("registrado por", "no puede estar vacío");
        }
    }
}
