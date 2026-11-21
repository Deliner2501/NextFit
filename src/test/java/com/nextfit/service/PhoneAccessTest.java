package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The carnet code only finds the member: it never registers an entry or an exit. */
class PhoneAccessTest {

    private PhoneAccess phone;
    private Member member;

    @BeforeEach
    void setUp() {
        MemberService members = new MemberService(new FakeMemberRepository(), new MemberValidator());
        member = members.register(FakeRepositories.newMember("1065123456"));
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 3);
        phone = new PhoneAccess(members, new AccessTokens(key));
    }

    @Test
    void theCodeOfTheCarnetFindsItsMember() {
        String token = phone.tokenOf(member);

        assertEquals(AccessTokens.LENGTH, token.length());
        assertEquals(member.getId(), phone.findMember(token).orElseThrow().getId());
    }

    @Test
    void lowerCaseAndSpacesAreTolerated() {
        String token = phone.tokenOf(member);

        assertTrue(phone.findMember(" " + token.toLowerCase() + " ").isPresent());
    }

    @Test
    void unknownOrEmptyCodesFindNobody() {
        assertFalse(phone.findMember("ZZZZZZZZZZ").isPresent());
        assertFalse(phone.findMember("").isPresent());
        assertFalse(phone.findMember(null).isPresent());
        assertFalse(phone.findMember("ABC").isPresent());
    }
}
