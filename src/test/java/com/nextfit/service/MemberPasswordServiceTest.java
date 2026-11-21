package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.Member;
import com.nextfit.service.MemberPasswordTestSupport.MemoryCredentials;
import com.nextfit.service.MemberPasswordTestSupport.RecordingAudit;
import com.nextfit.service.security.AttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberPasswordServiceTest {

    private MemoryCredentials credentials;
    private RecordingAudit audit;
    private MemberPasswordService service;
    private Member member;

    @BeforeEach
    void setUp() {
        credentials = new MemoryCredentials();
        audit = new RecordingAudit();
        service = new MemberPasswordService(credentials, AttemptLimiter.NONE, audit);
        member = MemberPasswordTestSupport.member(1, "1065123456", true);
    }

    @Test
    void createdPasswordIsStoredOnlyAsAHashAndChecksCorrectly() {
        service.create(member, "mi clave 2026", "mi clave 2026", "recepcion");

        assertTrue(service.hasPassword(1));
        assertFalse(credentials.find(1).orElseThrow().passwordHash().contains("mi clave 2026"));
        assertTrue(service.matches(member, "mi clave 2026"));
        assertFalse(service.matches(member, "otra clave 2026"));
        assertEquals(AuditAction.MEMBER_PASSWORD_CREATED, audit.actions.get(0));
        assertTrue(audit.details.get(0).startsWith("recepcion|"));
    }

    @Test
    void weakOrMismatchedPasswordsAreRefused() {
        assertThrows(InvalidDataException.class, () -> service.create(member, "abc", "abc", "r"));
        assertThrows(InvalidDataException.class, () -> service.create(member, "      ", "      ", "r"));
        assertThrows(InvalidDataException.class, () -> service.create(member, "x".repeat(65), "x".repeat(65), "r"));
        assertThrows(InvalidDataException.class, () -> service.create(member, "1065123456", "1065123456", "r"));
        assertThrows(InvalidDataException.class, () -> service.create(member, "clave123", "clave124", "r"));
        assertFalse(service.hasPassword(1));
    }

    @Test
    void aMemberWithAPasswordCannotCreateAnotherWithoutAReset() {
        service.create(member, "clave123", "clave123", "r");

        assertThrows(BusinessRuleException.class, () -> service.create(member, "nueva123", "nueva123", "r"));
        assertTrue(service.matches(member, "clave123"));
    }

    @Test
    void resetDeletesThePasswordSoANewOneCanBeCreated() {
        service.create(member, "clave123", "clave123", "r");

        service.reset(member, "admin");

        assertFalse(service.hasPassword(1));
        assertFalse(service.matches(member, "clave123"));
        assertEquals(AuditAction.MEMBER_PASSWORD_RESET, audit.actions.get(1));
        service.create(member, "nueva123", "nueva123", "r");
        assertTrue(service.matches(member, "nueva123"));
    }

    @Test
    void aMemberWithoutPasswordNeverMatches() {
        assertFalse(service.matches(member, "cualquiera"));
        assertFalse(service.matches(member, null));
    }
}
