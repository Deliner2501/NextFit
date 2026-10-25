package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Member;
import com.nextfit.service.FakeRepositories.FakeMemberRepository;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberServiceTest {

    private MemberService service;

    @BeforeEach
    void setUp() {
        service = new MemberService(new FakeMemberRepository(), new MemberValidator());
    }

    @Test
    void registerAssignsAnIdentifier() {
        Member stored = service.register(FakeRepositories.newMember("1065123456"));

        assertTrue(stored.getId() > 0);
        assertEquals(stored.getFullName(), service.findById(stored.getId()).getFullName());
    }

    @Test
    void registerRejectsADocumentThatIsAlreadyRegistered() {
        service.register(FakeRepositories.newMember("1065123456"));

        assertThrows(BusinessRuleException.class,
                () -> service.register(FakeRepositories.newMember("1065123456")));
    }

    @Test
    void registerRejectsInvalidData() {
        assertThrows(InvalidDataException.class,
                () -> service.register(FakeRepositories.newMember("12")));
    }

    @Test
    void findByIdFailsForAnUnknownMember() {
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99));
    }

    @Test
    void updateRejectsTheDocumentOfAnotherMember() {
        service.register(FakeRepositories.newMember("1111111"));
        Member second = service.register(FakeRepositories.newMember("2222222"));
        Member withTakenDocument = new Member(second.getId(), second.getFullName(), "1111111",
                second.getPhone(), second.getEmail(), second.getBirthDate(), second.getJoinDate(),
                second.getPhotoPath(), second.getEmergencyContact(), true);

        assertThrows(BusinessRuleException.class, () -> service.update(withTakenDocument));
    }

    @Test
    void deactivateAndReactivateChangeOnlyTheActiveFlag() {
        Member stored = service.register(FakeRepositories.newMember("1065123456"));

        assertFalse(service.deactivate(stored.getId()).isActive());
        assertTrue(service.reactivate(stored.getId()).isActive());
    }

    @Test
    void searchFindsByNameOrDocument() {
        service.register(FakeRepositories.newMember("1065123456"));

        assertEquals(1L, service.search("ana").size());
        assertEquals(1L, service.search("1065").size());
        assertEquals(0L, service.search("zzz").size());
    }
}