package com.nextfit.persistence;

import com.nextfit.model.MemberCredential;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Keeps the password hash of each member, one per member. */
public interface MemberCredentialRepository {

    /**
     * @param memberId the member
     * @return the credential, or empty when the member has not created a password
     */
    Optional<MemberCredential> find(long memberId);

    /**
     * @param members candidate member identifiers
     * @return the candidates who have a password
     */
    default Set<Long> credentialIds(List<Long> members) {
        return members.stream().filter(id -> find(id).isPresent()).collect(Collectors.toUnmodifiableSet());
    }

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
