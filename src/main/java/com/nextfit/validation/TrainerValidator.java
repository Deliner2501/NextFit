package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Trainer;

import java.time.LocalDate;

/**
 * Checks that the data of a {@link Trainer} is valid before it is stored.
 * Each field has its own method so screens can validate one field at a time.
 */
public class TrainerValidator {

    private static final int FULL_NAME_MAX = 60;
    private static final int DOCUMENT_MIN = 5;
    private static final int DOCUMENT_MAX = 12;
    private static final int PHONE_MIN = 7;
    private static final int PHONE_MAX = 15;
    private static final int SPECIALTY_MIN = 3;
    private static final int SPECIALTY_MAX = 40;
    private static final int MIN_AGE = 18;
    private static final int MAX_AGE = 80;

    /** Validates every field; throws InvalidDataException at the first invalid one. */
    public void validate(Trainer trainer) {
        validateFullName(trainer.getFullName());
        validateDocumentNumber(trainer.getDocumentNumber());
        validatePhone(trainer.getPhone());
        validateEmail(trainer.getEmail());
        validateBirthDate(trainer.getBirthDate());
        validateSpecialty(trainer.getSpecialty());
    }

    public void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidDataException("nombre completo", "no puede estar vacío");
        }
        if (fullName.length() > FULL_NAME_MAX) {
            throw new InvalidDataException("nombre completo",
                    "no puede superar los " + FULL_NAME_MAX + " caracteres");
        }
    }

    public void validateDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new InvalidDataException("documento", "no puede estar vacío");
        }
        if (!documentNumber.matches("\\d{" + DOCUMENT_MIN + "," + DOCUMENT_MAX + "}")) {
            throw new InvalidDataException("documento",
                    "debe tener entre " + DOCUMENT_MIN + " y " + DOCUMENT_MAX + " dígitos");
        }
    }

    public void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new InvalidDataException("teléfono", "no puede estar vacío");
        }
        if (!phone.matches("\\d{" + PHONE_MIN + "," + PHONE_MAX + "}")) {
            throw new InvalidDataException("teléfono",
                    "debe tener entre " + PHONE_MIN + " y " + PHONE_MAX + " dígitos");
        }
    }

    public void validateEmail(String email) {
        // El correo es opcional: solo se revisa el formato si viene escrito.
        if (email == null || email.isBlank()) {
            return;
        }
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new InvalidDataException("correo", "no tiene un formato válido");
        }
    }

    public void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new InvalidDataException("fecha de nacimiento", "es obligatoria");
        }
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            throw new InvalidDataException("fecha de nacimiento", "no puede ser futura");
        }
        // Un entrenador debe ser mayor de edad.
        if (birthDate.isAfter(today.minusYears(MIN_AGE)) || birthDate.isBefore(today.minusYears(MAX_AGE))) {
            throw new InvalidDataException("fecha de nacimiento",
                    "la edad debe estar entre " + MIN_AGE + " y " + MAX_AGE + " años");
        }
    }

    public void validateSpecialty(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            throw new InvalidDataException("especialidad", "no puede estar vacía");
        }
        if (specialty.length() < SPECIALTY_MIN || specialty.length() > SPECIALTY_MAX) {
            throw new InvalidDataException("especialidad",
                    "debe tener entre " + SPECIALTY_MIN + " y " + SPECIALTY_MAX + " caracteres");
        }
    }
}