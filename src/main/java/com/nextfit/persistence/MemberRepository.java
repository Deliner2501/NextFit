package com.nextfit.persistence;

import com.nextfit.model.Member;

import java.util.List;
import java.util.Optional;

/**
 * Storage of members. Services depend on this interface, so the CSV
 * implementation can be replaced by a database one without changing them.
 */
public interface MemberRepository {

    /**
     * Returns every stored member.
     *
     * @return the members
     */
    List<Member> findAll();

    /**
     * Looks a member up by identifier.
     *
     * @param id the identifier
     * @return the member, or empty when it does not exist
     */
    Optional<Member> findById(long id);

    /**
     * Looks a member up by national identity document number.
     *
     * @param documentNumber the document number
     * @return the member, or empty when it does not exist
     */
    Optional<Member> findByDocumentNumber(String documentNumber);

    /**
     * Inserts the member when its id is 0 (assigning a new one) or replaces the
     * stored member with the same id.
     *
     * @param member the member to store
     * @return the stored member, with its identifier
     */
    Member save(Member member);
}