package com.nextfit.service.security;

import com.nextfit.exceptions.BusinessRuleException;

/**
 * Limits the sign-in attempts. {@code AuthService} asks it before checking a password and
 * tells it how the attempt ended, so the way of limiting can change without touching the
 * sign-in rules (Open/Closed).
 */
public interface AttemptLimiter {

    /** A limiter that never blocks anybody; it is used when no limit is wanted (Null Object). */
    AttemptLimiter NONE = new AttemptLimiter() {
        @Override
        public void beforeAttempt(String username) {
            // EXPLICACIÓN: no hace nada a propósito, así AuthService no necesita preguntar si hay límite.
        }

        @Override
        public void failed(String username) {
            // no hace nada
        }

        @Override
        public void succeeded(String username) {
            // no hace nada
        }
    };

    /**
     * Checks that the user may try to sign in now.
     *
     * @param username the sign-in name that is being tried
     * @throws BusinessRuleException when the user is blocked
     */
    void beforeAttempt(String username);

    /**
     * Records a wrong attempt.
     *
     * @param username the sign-in name that was tried
     */
    void failed(String username);

    /**
     * Records a right attempt, which clears the failures of that name.
     *
     * @param username the sign-in name that signed in
     */
    void succeeded(String username);
}