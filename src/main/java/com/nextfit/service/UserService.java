package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import com.nextfit.service.audit.AuditLog;
import com.nextfit.util.PasswordHasher;
import com.nextfit.validation.UserValidator;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Rules for managing the accounts that can sign in: create them, change their
 * name and role, reset a password and enable or disable them. It guarantees
 * that the gym is never left without an active administrator, and it writes
 * every change in the audit log.
 */
public class UserService {

    private final UserRepository users;
    private final UserValidator validator;
    private final AuditLog audit;
    private final Supplier<String> actor;

    /**
     * Creates the service without audit.
     *
     * @param users     the user storage
     * @param validator the checker of user data
     */
    public UserService(UserRepository users, UserValidator validator) {
        this(users, validator, AuditLog.NONE, () -> "");
    }

    /**
     * Creates the service with an audit log.
     *
     * @param users     the user storage
     * @param validator the checker of user data
     * @param audit     where the changes are written
     * @param actor     gives the sign-in name of whoever is acting now
     */
    public UserService(UserRepository users, UserValidator validator, AuditLog audit, Supplier<String> actor) {
        this.users = users;
        this.validator = validator;
        this.audit = audit;
        this.actor = actor;
    }

    /**
     * Returns every user ordered by sign-in name.
     *
     * @return the users
     */
    public List<User> findAll() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(u -> u.getUsername().toLowerCase()))
                .toList();
    }

    /**
     * Creates an active user.
     *
     * @param username the sign-in name, unique ignoring case
     * @param fullName the name shown in the interface
     * @param password the clear text password; only its hash is stored
     * @param role     the access level
     * @return the stored user
     * @throws com.nextfit.exceptions.InvalidDataException when a field breaks its rule
     * @throws BusinessRuleException                       when the sign-in name is already taken
     */
    public User create(String username, String fullName, String password, Role role) {
        validator.validateNewUser(username, fullName, password, role);
        String cleanName = username.trim();
        if (users.findByUsername(cleanName).isPresent()) {
            throw new BusinessRuleException("Ya existe un usuario con el nombre '" + cleanName + "'");
        }
        User stored = users.save(new User(0, cleanName, fullName.trim(),
                PasswordHasher.hash(password), role, true));
        audit.record(actor.get(), AuditAction.USER_CREATED,
                "Usuario '" + stored.getUsername() + "' con rol " + role.getLabel());
        return stored;
    }

    /**
     * Changes the display name and the role of a user.
     *
     * @param id       the user to change
     * @param fullName the new display name
     * @param role     the new role
     * @return the stored user
     * @throws BusinessRuleException when it would leave the gym without an active administrator
     */
    public User update(long id, String fullName, Role role) {
        validator.validateFullName(fullName);
        validator.validateRole(role);
        User current = findById(id);
        if (current.isActive() && current.getRole() == Role.ADMIN && role != Role.ADMIN) {
            ensureAnotherActiveAdmin(id);
        }
        User stored = users.save(new User(id, current.getUsername(), fullName.trim(),
                current.getPasswordHash(), role, current.isActive()));
        audit.record(actor.get(), AuditAction.USER_UPDATED,
                "Usuario '" + stored.getUsername() + "', rol " + role.getLabel());
        return stored;
    }

    /**
     * Replaces the password of a user.
     *
     * @param id          the user
     * @param newPassword the new clear text password
     * @throws com.nextfit.exceptions.InvalidDataException when the password is too weak
     */
    public void resetPassword(long id, String newPassword) {
        validator.validatePassword(newPassword);
        User current = findById(id);
        users.save(new User(id, current.getUsername(), current.getFullName(),
                PasswordHasher.hash(newPassword), current.getRole(), current.isActive()));
        audit.record(actor.get(), AuditAction.PASSWORD_RESET, "Usuario '" + current.getUsername() + "'");
    }

    /**
     * Stops a user from signing in. Their data is kept.
     *
     * @param id           the user to disable
     * @param actingUserId the user who asks for it
     * @return the stored user
     * @throws BusinessRuleException when it is the own account or the last active administrator
     */
    public User deactivate(long id, long actingUserId) {
        if (id == actingUserId) {
            throw new BusinessRuleException("No puedes desactivar tu propia cuenta");
        }
        User current = findById(id);
        if (!current.isActive()) {
            return current;
        }
        if (current.getRole() == Role.ADMIN) {
            ensureAnotherActiveAdmin(id);
        }
        User stored = users.save(withActive(current, false));
        audit.record(actor.get(), AuditAction.USER_DEACTIVATED, "Usuario '" + stored.getUsername() + "'");
        return stored;
    }

    /**
     * Lets a disabled user sign in again.
     *
     * @param id the user to enable
     * @return the stored user
     */
    public User reactivate(long id) {
        User current = findById(id);
        if (current.isActive()) {
            return current;
        }
        User stored = users.save(withActive(current, true));
        audit.record(actor.get(), AuditAction.USER_REACTIVATED, "Usuario '" + stored.getUsername() + "'");
        return stored;
    }

    private User findById(long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("usuario", String.valueOf(id)));
    }

    private void ensureAnotherActiveAdmin(long excludedId) {
        boolean another = users.findAll().stream()
                .anyMatch(u -> u.getId() != excludedId && u.isActive() && u.getRole() == Role.ADMIN);
        if (!another) {
            throw new BusinessRuleException("Debe quedar al menos un administrador activo");
        }
    }

    private static User withActive(User user, boolean active) {
        return new User(user.getId(), user.getUsername(), user.getFullName(),
                user.getPasswordHash(), user.getRole(), active);
    }
}