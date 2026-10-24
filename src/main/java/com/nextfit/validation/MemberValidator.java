package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Member;

import java.time.LocalDate;

/**
 * Validates the data of a member before it is stored. It has no access to
 * files or screens, so it can be tested on its own.
 */
public class MemberValidator {

    private static final int FULL_NAME_MAX = 60;
    private static final int DOCUMENT_MIN = 5;
    private static final int DOCUMENT_MAX = 12;
    private static final int PHONE_MIN = 7;
    private static final int PHONE_MAX = 15;
    private static final int EMERGENCY_CONTACT_MAX = 80;
    private static final int MIN_AGE = 12;
    private static final int MAX_AGE = 100;

    /**
     * Validates every field of a member.
     *
     * @param member the member to check
     * @throws InvalidDataException when any field breaks its rule
     */
    public void validate(Member member) {
        validateFullName(member.getFullName());
        validateDocumentNumber(member.getDocumentNumber());
        validatePhone(member.getPhone());
        validateEmail(member.getEmail());
        validateBirthDate(member.getBirthDate());
        validateJoinDate(member.getJoinDate());
        validateEmergencyContact(member.getEmergencyContact());
    }

    /**
     * Validates the full name.
     *
     * @param fullName the full name
     * @throws InvalidDataException when it is empty or too long
     */
    public void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidDataException("nombre completo", "no puede estar vacío");
        }
        if (fullName.length() > FULL_NAME_MAX) {
            throw new InvalidDataException("nombre completo",
                    "no puede superar los " + FULL_NAME_MAX + " caracteres");
        }
    }

    /**
     * Validates the document number: only digits, from 5 to 12.
     *
     * @param documentNumber the document number
     * @throws InvalidDataException when it is empty or not made of 5 to 12 digits
     */
    public void validateDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new InvalidDataException("documento", "no puede estar vacío");
        }
        if (!documentNumber.matches("\\d{" + DOCUMENT_MIN + "," + DOCUMENT_MAX + "}")) {
            throw new InvalidDataException("documento",
                    "debe tener entre " + DOCUMENT_MIN + " y " + DOCUMENT_MAX + " dígitos");
        }
    }

    /**
     * Validates the phone number: only digits, from 7 to 15.
     *
     * @param phone the phone number
     * @throws InvalidDataException when it is empty or not made of 7 to 15 digits
     */
    public void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new InvalidDataException("teléfono", "no puede estar vacío");
        }
        if (!phone.matches("\\d{" + PHONE_MIN + "," + PHONE_MAX + "}")) {
            throw new InvalidDataException("teléfono",
                    "debe tener entre " + PHONE_MIN + " y " + PHONE_MAX + " dígitos");
        }
    }

    /**
     * Validates the email. It is optional, but when it is given it must look like an email.
     *
     * @param email the email address, or an empty text
     * @throws InvalidDataException when it is given and malformed
     */
    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new InvalidDataException("correo", "no tiene un formato válido");
        }
    }

    /**
     * Validates the date of birth: it must exist, not be in the future and
     * give an age from 12 to 100 years.
     *
     * @param birthDate the date of birth
     * @throws InvalidDataException when it is missing or out of range
     */
    public void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new InvalidDataException("fecha de nacimiento", "es obligatoria");
        }
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            throw new InvalidDataException("fecha de nacimiento", "no puede ser futura");
        }
        LocalDate youngest = today.minusYears(MIN_AGE);
        LocalDate oldest = today.minusYears(MAX_AGE);
        if (birthDate.isAfter(youngest) || birthDate.isBefore(oldest)) {
            throw new InvalidDataException("fecha de nacimiento",
                    "la edad debe estar entre " + MIN_AGE + " y " + MAX_AGE + " años");
        }
    }

    /**
     * Validates the date the member joined the gym.
     *
     * @param joinDate the join date
     * @throws InvalidDataException when it is missing or in the future
     */
    public void validateJoinDate(LocalDate joinDate) {
        if (joinDate == null) {
            throw new InvalidDataException("fecha de ingreso", "es obligatoria");
        }
        if (joinDate.isAfter(LocalDate.now())) {
            throw new InvalidDataException("fecha de ingreso", "no puede ser futura");
        }
    }

    /**
     * Validates the emergency contact. It is optional, but it cannot be too long.
     *
     * @param emergencyContact the emergency contact, or an empty text
     * @throws InvalidDataException when it is too long
     */
    public void validateEmergencyContact(String emergencyContact) {
        if (emergencyContact != null && emergencyContact.length() > EMERGENCY_CONTACT_MAX) {
            throw new InvalidDataException("contacto de emergencia",
                    "no puede superar los " + EMERGENCY_CONTACT_MAX + " caracteres");
        }
    }
}