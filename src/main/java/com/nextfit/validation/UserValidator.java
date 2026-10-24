package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Role;

/**
 * Validates the data of a user before it is stored. It has no access to
 * files or screens, so it can be tested on its own.
 */
public class UserValidator {

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 20;
    private static final int FULL_NAME_MAX = 60;
    private static final int PASSWORD_MIN = 8;

    /**
     * Validates every field needed to create a user.
     *
     * @param username the sign-in name
     * @param fullName the display name
     * @param password the clear text password
     * @param role     the access level
     * @throws InvalidDataException when any field breaks its rule
     */
    public void validateNewUser(String username, String fullName, String password, Role role) {
        validateUsername(username);
        validateFullName(fullName);
        validatePassword(password);
        validateRole(role);
    }

    /**
     * Validates the sign-in name: 3 to 20 letters, digits, dots or underscores.
     *
     * @param username the sign-in name
     * @throws InvalidDataException when it is not valid
     */
    public void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidDataException("usuario", "no puede estar vacío");
        }
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            throw new InvalidDataException("usuario",
                    "debe tener entre " + USERNAME_MIN + " y " + USERNAME_MAX + " caracteres");
        }
        if (!username.matches("[A-Za-z0-9._]+")) {
            throw new InvalidDataException("usuario", "solo puede tener letras, números, punto y guion bajo");
        }
    }

    /**
     * Validates the display name.
     *
     * @param fullName the display name
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
     * Validates the password: at least 8 characters, with a letter and a digit.
     *
     * @param password the clear text password
     * @throws InvalidDataException when it is too weak
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new InvalidDataException("contraseña", "debe tener al menos " + PASSWORD_MIN + " caracteres");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new InvalidDataException("contraseña", "debe combinar letras y números");
        }
    }

    /**
     * Validates that a role was chosen.
     *
     * @param role the access level
     * @throws InvalidDataException when it is null
     */
    public void validateRole(Role role) {
        if (role == null) {
            throw new InvalidDataException("rol", "debe seleccionar un rol");
        }
    }
}