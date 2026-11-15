package com.nextfit.service;

import com.nextfit.model.AuditAction;
import com.nextfit.model.Member;
import com.nextfit.service.AccessResult.Outcome;
import com.nextfit.service.MemberPasswordTestSupport.MemoryCredentials;
import com.nextfit.service.MemberPasswordTestSupport.MovingClock;
import com.nextfit.service.MemberPasswordTestSupport.RecordingAudit;
import com.nextfit.service.security.InMemoryAttemptLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfAccessServiceTest {

    private MovingClock clock;
    private RecordingAudit audit;
    private MemberPasswordService passwords;
    private SelfAccessService access;
    private Member member;
    private Member inactive;
    private final List<Member> admitted = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clock = new MovingClock();
        audit = new RecordingAudit();
        InMemoryAttemptLimiter limiter = new InMemoryAttemptLimiter(3, 5, clock);
        passwords = new MemberPasswordService(new MemoryCredentials(), limiter, audit);
        member = MemberPasswordTestSupport.member(1, "1065123456", true);
        inactive = MemberPasswordTestSupport.member(2, "1065999999", false);
        Member noPassword = MemberPasswordTestSupport.member(3, "1065000003", true);
        passwords.create(member, "clave segura", "clave segura", "r");
        passwords.create(inactive, "clave segura", "clave segura", "r");
        access = new SelfAccessService(doc -> List.of(member, inactive, noPassword).stream()
                .filter(m -> m.getDocumentNumber().equals(doc)).findFirst(),
                passwords, limiter, audit,
                m -> {
                    admitted.add(m);
                    return new AccessResult(Outcome.ENTRY, m, "Bienvenido", null, -1);
                },
                reason -> new AccessResult(Outcome.DENIED, null, reason, null, -1));
    }

    @Test
    void rightDocumentAndPasswordLetTheMemberIn() {
        AccessResult result = access.enter(" 1065123456 ", "clave segura");

        assertEquals(Outcome.ENTRY, result.outcome());
        assertEquals(List.of(member), admitted);
    }

    @Test
    void wrongPasswordAndUnknownDocumentGetTheSameAnswer() {
        AccessResult wrongPassword = access.enter("1065123456", "mala clave");
        AccessResult unknown = access.enter("1060000000", "clave segura");

        assertEquals(Outcome.DENIED, wrongPassword.outcome());
        assertEquals(SelfAccessService.WRONG, wrongPassword.message());
        assertEquals(SelfAccessService.WRONG, unknown.message());
        assertTrue(admitted.isEmpty());
        assertEquals(AuditAction.MEMBER_LOGIN_FAILED, audit.actions.get(audit.actions.size() - 1));
    }

    @Test
    void emptyFieldsAreRefusedWithoutCountingAsAnAttempt() {
        assertEquals(Outcome.DENIED, access.enter("", "x").outcome());
        assertEquals(Outcome.DENIED, access.enter("1065123456", "").outcome());
        assertEquals(Outcome.DENIED, access.enter(null, null).outcome());
        assertEquals(Outcome.ENTRY, access.enter("1065123456", "clave segura").outcome());
    }

    @Test
    void aMemberWithoutPasswordIsToldToAskReception() {
        AccessResult result = access.enter("1065000003", "lo que sea");

        assertEquals(Outcome.DENIED, result.outcome());
        assertTrue(result.message().contains("recepción"));
    }

    @Test
    void anInactiveMemberCannotEnterEvenWithTheRightPassword() {
        AccessResult result = access.enter("1065999999", "clave segura");

        assertEquals(Outcome.DENIED, result.outcome());
        assertTrue(result.message().startsWith("Socio inactivo"));
        assertTrue(admitted.isEmpty());
    }

    @Test
    void tooManyWrongPasswordsBlockTheDocumentEvenForTheRightPassword() {
        for (int i = 0; i < 3; i++) {
            access.enter("1065123456", "mala clave " + i);
        }

        AccessResult blocked = access.enter("1065123456", "clave segura");

        assertEquals(Outcome.DENIED, blocked.outcome());
        assertTrue(blocked.message().contains("Demasiados intentos"));
        assertEquals(AuditAction.MEMBER_LOCKOUT, audit.actions.get(audit.actions.size() - 1));
        assertTrue(admitted.isEmpty());

        clock.advanceMinutes(6);
        assertEquals(Outcome.ENTRY, access.enter("1065123456", "clave segura").outcome());
    }

    @Test
    void resetLiftsTheBlock() {
        for (int i = 0; i < 3; i++) {
            access.enter("1065123456", "mala clave " + i);
        }

        passwords.reset(member, "admin");
        passwords.create(member, "otra clave nueva", "otra clave nueva", "admin");

        assertEquals(Outcome.ENTRY, access.enter("1065123456", "otra clave nueva").outcome());
    }
}
