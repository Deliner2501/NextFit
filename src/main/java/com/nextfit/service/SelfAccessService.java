package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.Member;
import com.nextfit.service.audit.AuditLog;
import com.nextfit.service.security.AttemptLimiter;

import java.util.Optional;
import java.util.function.Function;

/**
 * Lets a member in with the document number and the password he created. Every refusal gives the same
 * answer for a wrong document and a wrong password, so nobody can find out which documents exist, and a
 * document is blocked for a few minutes after too many wrong passwords.
 */
public class SelfAccessService {

    public static final String WRONG = "Documento o contraseña incorrectos.";

    private final Function<String, Optional<Member>> findByDocument;
    private final MemberPasswordService passwords;
    private final AttemptLimiter limiter;
    private final AuditLog audit;
    private final Function<Member, AccessResult> admit;
    private final Function<String, AccessResult> deny;

    /**
     * @param findByDocument finds a member by the document number
     * @param passwords      checks the password
     * @param limiter        blocks a document after too many wrong passwords
     * @param audit          where the failed attempts are written
     * @param admit          registers the entry or the exit of a member (see {@link AccessService#processMember})
     * @param deny           writes a denied access with the reason
     */
    public SelfAccessService(Function<String, Optional<Member>> findByDocument, MemberPasswordService passwords,
                             AttemptLimiter limiter, AuditLog audit,
                             Function<Member, AccessResult> admit, Function<String, AccessResult> deny) {
        this.findByDocument = findByDocument;
        this.passwords = passwords;
        this.limiter = limiter;
        this.audit = audit;
        this.admit = admit;
        this.deny = deny;
    }

    /**
     * Registers the entry or the exit of the member who typed a document and a password.
     *
     * @param document the document number
     * @param password the password
     * @return the entry, the exit, or a denial with the reason
     */
    public AccessResult enter(String document, String password) {
        if (document == null || document.isBlank() || password == null || password.isEmpty()) {
            return deny.apply("Escribe tu documento y tu contraseña.");
        }
        String doc = document.trim();
        try {
            limiter.beforeAttempt(doc);
        } catch (BusinessRuleException blocked) {
            audit.record("documento " + doc, AuditAction.MEMBER_LOCKOUT, "Intento mientras estaba bloqueado");
            return deny.apply(blocked.getMessage());
        }
        Optional<Member> member = findByDocument.apply(doc);
        if (member.isPresent() && !passwords.hasPassword(member.get().getId())) {
            return deny.apply("Todavía no tienes contraseña. Pídele a recepción que te ayude a crearla.");
        }
        if (member.isEmpty() || !passwords.matches(member.get(), password)) {
            limiter.failed(doc);
            audit.record("documento " + doc, AuditAction.MEMBER_LOGIN_FAILED, "Documento o contraseña incorrectos");
            return deny.apply(WRONG);
        }
        limiter.succeeded(doc);
        if (!member.get().isActive()) {
            return deny.apply("Socio inactivo: " + member.get().getFullName());
        }
        return admit.apply(member.get());
    }
}
