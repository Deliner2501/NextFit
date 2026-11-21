package com.nextfit.service;

import com.nextfit.model.Member;

import java.util.Optional;

/**
 * Links a member with the code of his carnet. The carnet QR opens the personal portal of the member
 * (read only): the code identifies the member to show his progress, but it never registers an entry
 * or an exit.
 */
public class PhoneAccess {

    private final MemberService memberService;
    private final AccessTokens tokens;

    /**
     * Creates the service.
     *
     * @param memberService the source of members
     * @param tokens        builds and checks the codes of the carnets
     */
    public PhoneAccess(MemberService memberService, AccessTokens tokens) {
        this.memberService = memberService;
        this.tokens = tokens;
    }

    /**
     * Returns the access code of a member, to build the carnet QR.
     *
     * @param member the member
     * @return the code
     */
    public String tokenOf(Member member) {
        return tokens.tokenFor(member.getDocumentNumber());
    }

    /**
     * Finds the member that owns an access code.
     *
     * @param token the code read from the QR
     * @return the member, or empty when the code belongs to nobody
     */
    public Optional<Member> findMember(String token) {
        if (AccessTokens.normalize(token).isEmpty()) {
            return Optional.empty();
        }
        return memberService.findAll().stream()
                .filter(m -> tokens.matches(m.getDocumentNumber(), token))
                .findFirst();
    }
}
