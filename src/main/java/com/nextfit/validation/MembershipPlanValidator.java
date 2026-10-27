package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.MembershipPlan;

/**
 * Validates the data of a membership plan before it is stored. It has no
 * access to files or screens, so it can be tested on its own.
 */
public class MembershipPlanValidator {

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 40;
    private static final int DESCRIPTION_MAX = 120;
    private static final int DURATION_MAX_DAYS = 730;
    private static final long PRICE_MAX = 50_000_000L;

    /**
     * Validates every field of a plan.
     *
     * @param plan the plan to check
     * @throws InvalidDataException when any field breaks its rule
     */
    public void validate(MembershipPlan plan) {
        validateName(plan.getName());
        validateDescription(plan.getDescription());
        validateDuration(plan.getDurationDays());
        validatePrice(plan.getPrice());
    }

    /**
     * Validates the name: 3 to 40 characters.
     *
     * @param name the name
     * @throws InvalidDataException when it is empty or has the wrong length
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDataException("nombre", "no puede estar vacío");
        }
        String trimmed = name.trim();
        if (trimmed.length() < NAME_MIN || trimmed.length() > NAME_MAX) {
            throw new InvalidDataException("nombre",
                    "debe tener entre " + NAME_MIN + " y " + NAME_MAX + " caracteres");
        }
    }

    /**
     * Validates the description: optional, up to 120 characters.
     *
     * @param description the description, or null
     * @throws InvalidDataException when it is too long
     */
    public void validateDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX) {
            throw new InvalidDataException("descripción",
                    "no puede superar los " + DESCRIPTION_MAX + " caracteres");
        }
    }

    /**
     * Validates the duration: from 1 day to two years.
     *
     * @param durationDays the number of days
     * @throws InvalidDataException when it is out of range
     */
    public void validateDuration(int durationDays) {
        if (durationDays < 1 || durationDays > DURATION_MAX_DAYS) {
            throw new InvalidDataException("duración",
                    "debe estar entre 1 y " + DURATION_MAX_DAYS + " días");
        }
    }

    /**
     * Validates the price: greater than zero and not absurdly high.
     *
     * @param price the price in pesos
     * @throws InvalidDataException when it is out of range
     */
    public void validatePrice(long price) {
        if (price < 1 || price > PRICE_MAX) {
            throw new InvalidDataException("precio",
                    "debe ser mayor que cero y no superar " + PRICE_MAX + " pesos");
        }
    }
}
