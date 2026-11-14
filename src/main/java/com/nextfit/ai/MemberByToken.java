package com.nextfit.ai;

import com.nextfit.model.Member;

import java.util.Optional;

/**
 * Finds the member that owns the access code of a carnet. The phone chat uses it to know who is
 * asking, so the identity always comes from the code and never from what is written in the chat.
 */
@FunctionalInterface
public interface MemberByToken {

    /**
     * Finds the member of a code.
     *
     * @param token the access code of the carnet
     * @return the member, or empty when the code belongs to nobody
     */
    Optional<Member> find(String token);
}