package com.nextfit.persistence;

import com.nextfit.model.MemberCredential;

import java.util.Optional;

/** Keeps the password hash of each member, one per member. */
public interface MemberCredentialRepository {

    /**
     * @param memberId the member
     * @return the credential, or empty when the member has not created a password
     */
    Optional<MemberCredential> find(long memberId);

    /**
     * Saves the credential, replacing the one the member had.
     *
     * @param credential the credential to keep
     */
    void save(MemberCredential credential);

    /**
     * Deletes the password of a member; it does nothing when there is none.
     *
     * @param memberId the member
     */
    void delete(long memberId);
}
