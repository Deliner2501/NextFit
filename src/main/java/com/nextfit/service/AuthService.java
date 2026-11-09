package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.audit.AuditLog;
import com.nextfit.service.security.AttemptLimiter;
import com.nextfit.util.PasswordHasher;

import java.util.Optional;

/**
 * Sign-in rules. It knows nothing about screens or files: it only uses a
 * {@link UserRepository}, an {@link AttemptLimiter}, which blocks a name that
 * fails too often, and an {@link AuditLog}, where it writes what happened.
 */
public class AuthService {

    /** Sign-in name of the administrator created on the first run. */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    /** Initial password of that administrator; it should be changed after the first sign-in. */
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final UserRepository users;
    private final AttemptLimiter limiter;
    private final AuditLog audit;

    /**
     * Creates the service without a limit of attempts and without audit.
     *
     * @param users the user storage
     */
    public AuthService(UserRepository users) {
        this(users, AttemptLimiter.NONE);
    }

    /**
     * Creates the service with a limit of attempts and without audit.
     *
     * @param users   the user storage
     * @param limiter blocks a name after too many wrong attempts
     */
    public AuthService(UserRepository users, AttemptLimiter limiter) {
        this(users, limiter, AuditLog.NONE);
    }

    /**
     * Creates the service with a limit of attempts and an audit log.
     *
     * @param users   the user storage
     * @param limiter blocks a name after too many wrong attempts
     * @param audit   where the sign-in events are written
     */
    public AuthService(UserRepository users, AttemptLimiter limiter, AuditLog audit) {
        this.users = users;
        this.limiter = limiter;
        this.audit = audit;
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
     * @throws BusinessRuleException when the credentials are wrong, the name is blocked for too
     *                               many wrong attempts, or the user is disabled
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new InvalidDataException("usuario", "no puede estar vacío");
        }
        if (password == null || password.isEmpty()) {
            throw new InvalidDataException("contraseña", "no puede estar vacía");
        }
        String name = username.trim();
        try {
            limiter.beforeAttempt(name);
        } catch (BusinessRuleException blocked) {
            audit.record(name, AuditAction.LOCKOUT, "Intento de entrar mientras el usuario estaba bloqueado");
            throw blocked;
        }
        Optional<User> found = users.findByUsername(name)
                .filter(u -> PasswordHasher.verify(password, u.getPasswordHash()));
        if (found.isEmpty()) {
            limiter.failed(name);
            audit.record(name, AuditAction.LOGIN_FAILED, "Usuario o contraseña incorrectos");
            throw new BusinessRuleException("Usuario o contraseña incorrectos");
        }
        limiter.succeeded(name);
        User user = found.get();
        if (!user.isActive()) {
            audit.record(user.getUsername(), AuditAction.LOGIN_FAILED, "El usuario está desactivado");
            throw new BusinessRuleException("Este usuario está desactivado, consulta con el administrador");
        }
        audit.record(user.getUsername(), AuditAction.LOGIN, "Rol: " + user.getRole().getLabel());
        return user;
    }
        /**
     * Ends the session of the user who asked to sign out and writes it in the audit log.
     *
     * @param session the session to end
     */
    public void logout(Session session) {
        end(session, AuditAction.LOGOUT, "");
    }

    /**
     * Ends the session because the user was inactive for too long and writes it in the audit log.
     *
     * @param session the session to end
     */
    public void expire(Session session) {
        end(session, AuditAction.SESSION_EXPIRED, "Sin actividad");
    }

    private void end(Session session, AuditAction action, String detail) {
        session.getCurrentUser().ifPresent(user -> audit.record(user.getUsername(), action, detail));
        session.end();
    }
}