package com.nextfit.ai;

import com.nextfit.model.Role;

/**
 * Who is asking the assistant. The staff roles and the member are different audiences because
 * each one may use different tools: the trainer does not see money, and the member only sees
 * what belongs to himself.
 */
public enum AiAudience {

    ADMIN,
    RECEPTION,
    TRAINER,
    MEMBER;

    /**
     * Returns the audience of a staff role.
     *
     * @param role the role of the user who signed in
     * @return the audience
     */
    public static AiAudience of(Role role) {
        return switch (role) {
            case ADMIN -> ADMIN;
            case RECEPTION -> RECEPTION;
            case TRAINER -> TRAINER;
        };
    }
}