package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Member;
import com.nextfit.persistence.MemberRepository;
import com.nextfit.validation.MemberValidator;

import java.util.List;

/**
 * Rules of the gym members: who can be registered, changed or deactivated.
 * It knows nothing about screens or files: it only uses a
 * {@link MemberRepository} and a {@link MemberValidator}.
 */
public class MemberService {

    private final MemberRepository members;
    private final MemberValidator validator;

    /**
     * Creates the service.
     *
     * @param members   the member storage
     * @param validator the checker of member data
     */
    public MemberService(MemberRepository members, MemberValidator validator) {
        this.members = members;
        this.validator = validator;
    }

    /**
     * Returns every member.
     *
     * @return the members
     */
    public List<Member> findAll() {
        return members.findAll();
    }

    /**
     * Looks a member up by identifier.
     *
     * @param id the identifier
     * @return the member
     * @throws ResourceNotFoundException when no member has that identifier
     */
    public Member findById(long id) {
        return members.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("socio", String.valueOf(id)));
    }

    /**
     * Searches members whose name contains the text or whose document number
     * contains it. An empty text returns every member.
     *
     * @param text the text to look for
     * @return the matching members
     */
    public List<Member> search(String text) {
        if (text == null || text.isBlank()) {
            return findAll();
        }
        String wanted = text.trim().toLowerCase();
        return members.findAll().stream()
                .filter(m -> m.getFullName().toLowerCase().contains(wanted)
                        || m.getDocumentNumber().contains(wanted))
                .toList();
    }

    /**
     * Registers a new member. The identifier of the given member is ignored:
     * the storage assigns a new one.
     *
     * @param member the member to register
     * @return the stored member, with its identifier
     * @throws com.nextfit.exceptions.InvalidDataException when some field is not valid
     * @throws BusinessRuleException                       when the document is already registered
     */
    public Member register(Member member) {
        validator.validate(member);
        ensureDocumentIsFree(member);
        return members.save(member.withId(0));
    }

    /**
     * Changes the data of an existing member.
     *
     * @param member the member with its new data
     * @return the stored member
     * @throws ResourceNotFoundException                   when the member does not exist
     * @throws com.nextfit.exceptions.InvalidDataException when some field is not valid
     * @throws BusinessRuleException                       when the document belongs to another member
     */
    public Member update(Member member) {
        findById(member.getId());
        validator.validate(member);
        ensureDocumentIsFree(member);
        return members.save(member);
    }

    /**
     * Marks a member as no longer registered in the gym. The data is kept.
     *
     * @param id the identifier of the member
     * @return the updated member
     * @throws ResourceNotFoundException when the member does not exist
     */
    public Member deactivate(long id) {
        return members.save(findById(id).withActive(false));
    }

    /**
     * Marks a member as registered in the gym again.
     *
     * @param id the identifier of the member
     * @return the updated member
     * @throws ResourceNotFoundException when the member does not exist
     */
    public Member reactivate(long id) {
        return members.save(findById(id).withActive(true));
    }

    private void ensureDocumentIsFree(Member member) {
        boolean usedByAnother = members.findByDocumentNumber(member.getDocumentNumber())
                .filter(other -> other.getId() != member.getId())
                .isPresent();
        if (usedByAnother) {
            throw new BusinessRuleException(
                    "Ya existe un socio registrado con el documento " + member.getDocumentNumber());
        }
    }
}