package com.nextfit.ai;

import com.nextfit.model.Role;

/**
 * The person who asks the assistant. Tools use it to decide what they may answer: a member only
 * gets his own data, so his identifier travels here and never comes from what he types.
 *
 * @param audience the kind of person
 * @param memberId the member, or 0 when the person is staff
 * @param name     the name, to greet and to write in the audit log
 */
public record AiUser(AiAudience audience, long memberId, String name) {

    /**
     * Creates the user for a person of the staff.
     *
     * @param role the role of the user who signed in
     * @param name the sign-in name
     * @return the user
     */
    public static AiUser staff(Role role, String name) {
        return new AiUser(AiAudience.of(role), 0, name);
    }

    /**
     * Creates the user for a member who asks from the phone.
     *
     * @param memberId the member, identified by his card code
     * @param name     the name of the member
     * @return the user
     */
    public static AiUser member(long memberId, String name) {
        return new AiUser(AiAudience.MEMBER, memberId, name);
    }
}