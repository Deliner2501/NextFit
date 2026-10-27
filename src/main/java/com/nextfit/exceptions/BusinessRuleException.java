package com.nextfit.exceptions;

/**
 * Thrown when an operation violates a business rule of the gym,
 * such as registering a member who already exists, charging a
 * subscription that is still active, or assigning a routine to
 * a member without an active membership.
 */
public class BusinessRuleException extends NextFitException {

    private static final String ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    /**
     * Creates the exception with a message describing the violated rule.
     *
     * @param message the description of the business rule that was violated
     */
    public BusinessRuleException(String message) {
        super(message, ERROR_CODE);
    }
}