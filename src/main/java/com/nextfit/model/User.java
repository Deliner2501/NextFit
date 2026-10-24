package com.nextfit.model;

/**
 * A person who can sign in to NextFit. The password is never stored in
 * clear text: only its salted hash is kept.
 */
public class User {

    private final long id;
    private final String username;
    private final String fullName;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    /**
     * Creates a user.
     *
     * @param id           the identifier, or 0 when it has not been assigned yet
     * @param username     the unique sign-in name
     * @param fullName     the name shown in the interface
     * @param passwordHash the salted password hash
     * @param role         the access level
     * @param active       whether the user may sign in
     */
    public User(long id, String username, String fullName, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    /**
     * Returns a copy of this user with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public User withId(long newId) {
        return new User(newId, username, fullName, passwordHash, role, active);
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}