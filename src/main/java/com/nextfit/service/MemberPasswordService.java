package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.Member;
import com.nextfit.model.MemberCredential;
import com.nextfit.persistence.MemberCredentialRepository;
import com.nextfit.service.audit.AuditLog;
import com.nextfit.service.security.AttemptLimiter;
import com.nextfit.util.PasswordHasher;

/**
 * The passwords that members use to enter the gym by themselves. The member types the password on the
 * reception screen with a member of the staff beside, so a staff user is always the one who asks for the
 * change and the one written in the audit log. Only the salted hash is stored.
 */
public class MemberPasswordService {

    public static final int MIN_LENGTH = 6;
    public static final int MAX_LENGTH = 64;

    private final MemberCredentialRepository credentials;
    private final AttemptLimiter limiter;
    private final AuditLog audit;

    /**
     * @param credentials where the password hashes are kept
     * @param limiter     blocks a document after too many wrong passwords
     * @param audit       where the changes are written
     */
    public MemberPasswordService(MemberCredentialRepository credentials, AttemptLimiter limiter, AuditLog audit) {
        this.credentials = credentials;
        this.limiter = limiter;
        this.audit = audit;
    }

    /**
     * @param memberId the member
     * @return true when the member already created a password
     */
    public boolean hasPassword(long memberId) {
        return credentials.find(memberId).isPresent();
    }

    /**
     * Creates the first password of a member.
     *
     * @param member       the member
     * @param password     the password typed by the member
     * @param confirmation the same password typed again
     * @param actor        the staff user who is present, for the audit log
     * @throws InvalidDataException  when the password is too short, too long, blank, equal to the
     *                               document number or different from the confirmation
     * @throws BusinessRuleException when the member already has a password
     */
    public void create(Member member, String password, String confirmation, String actor) {
        if (hasPassword(member.getId())) {
            throw new BusinessRuleException("El socio ya tiene contraseña. Restablécela primero para crear una nueva.");
        }
        validate(member, password, confirmation);
        credentials.save(new MemberCredential(member.getId(), PasswordHasher.hash(password)));
        audit.record(actor, AuditAction.MEMBER_PASSWORD_CREATED, member.getFullName());
    }

    /**
     * Deletes the password of a member (he forgot it); he creates a new one afterwards. It also
     * lifts a block caused by wrong passwords.
     *
     * @param member the member
     * @param actor  the staff user, for the audit log
     */
    public void reset(Member member, String actor) {
        credentials.delete(member.getId());
        limiter.succeeded(member.getDocumentNumber());
        audit.record(actor, AuditAction.MEMBER_PASSWORD_RESET, member.getFullName());
    }

    /**
     * Checks a password.
     *
     * @param member   the member
     * @param password what was typed
     * @return true when the member has a password and it is this one
     */
    public boolean matches(Member member, String password) {
        return credentials.find(member.getId())
                .map(credential -> PasswordHasher.verify(password, credential.passwordHash()))
                .orElse(false);
    }

    private static void validate(Member member, String password, String confirmation) {
        if (password == null || password.isBlank()) {
            throw new InvalidDataException("contraseña", "no puede estar vacía");
        }
        if (password.length() < MIN_LENGTH) {
            throw new InvalidDataException("contraseña", "debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (password.length() > MAX_LENGTH) {
            throw new InvalidDataException("contraseña", "no puede tener más de " + MAX_LENGTH + " caracteres");
        }
        if (password.trim().equals(member.getDocumentNumber().trim())) {
            throw new InvalidDataException("contraseña", "no puede ser tu número de documento");
        }
        if (!password.equals(confirmation)) {
            throw new InvalidDataException("confirmación", "no coincide con la contraseña");
        }
    }
}
