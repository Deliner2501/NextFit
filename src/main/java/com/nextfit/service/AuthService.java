package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.util.PasswordHasher;

/**
 * Sign-in rules. It knows nothing about screens or files: it only uses a
 * {@link UserRepository}.
 */
public class AuthService {

    /** Sign-in name of the administrator created on the first run. */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    /** Initial password of that administrator; it should be changed after the first sign-in. */
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepository users;

    /**
     * Creates the service.
     *
     * @param users the user storage
     */
    public AuthService(UserRepository users) {
        this.users = users;
    }

    /**
     * Creates the initial administrator when there are no users, so a fresh
     * installation can always be entered.
     */
    public void ensureDefaultAdmin() {
        if (users.findAll().isEmpty()) {
            users.save(new User(0, DEFAULT_ADMIN_USERNAME, "Administrador",
                    PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD), Role.ADMIN, true));
        }
    }

    /**
     * Checks the credentials and returns the user.
     *
     * @param username the sign-in name
     * @param password the clear text password
     * @return the user who signed in
     * @throws InvalidDataException  when the username or the password is empty
     * @throws BusinessRuleException when the credentials are wrong or the user is disabled
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new InvalidDataException("usuario", "no puede estar vacío");
        }
        if (password == null || password.isEmpty()) {
            throw new InvalidDataException("contraseña", "no puede estar vacía");
        }
        User user = users.findByUsername(username.trim())
                .filter(u -> PasswordHasher.verify(password, u.getPasswordHash()))
                .orElseThrow(() -> new BusinessRuleException("Usuario o contraseña incorrectos"));
        if (!user.isActive()) {
            throw new BusinessRuleException("Este usuario está desactivado, consulta con el administrador");
        }
        return user;
    }
}