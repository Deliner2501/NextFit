package com.nextfit.service;

import com.nextfit.model.Role;
import com.nextfit.model.User;

import java.util.Optional;

/**
 * Holds the user who is currently signed in. A single instance is created in
 * {@code Main} and passed to whoever needs it, so there is no global state.
 */
public class Session {

    private User currentUser;

    /**
     * Starts the session of a user.
     *
     * @param user the user who signed in
     */
    public void start(User user) {
        this.currentUser = user;
    }

    /** Ends the current session. */
    public void end() {
        this.currentUser = null;
    }

    /**
     * Returns the signed-in user.
     *
     * @return the user, or empty when nobody is signed in
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Tells whether the signed-in user has the given role.
     *
     * @param role the role to check
     * @return true when somebody is signed in with that role
     */
    public boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }
}