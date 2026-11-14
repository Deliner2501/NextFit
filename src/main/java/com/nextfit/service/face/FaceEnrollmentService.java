package com.nextfit.service.face;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.FaceTemplate;
import com.nextfit.model.Member;
import com.nextfit.persistence.FaceTemplateRepository;
import com.nextfit.service.audit.AuditLog;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers, replaces and deletes the faces of the members. A face is stored only after the member
 * accepted it, from several pictures that must look like the same person and that do not look like any
 * other member. Every change is written to the audit log.
 */
public class FaceEnrollmentService {

    /** Pictures needed to register a face. */
    public static final int MIN_SAMPLES = 3;
    /** Two pictures of the same person must be at least this similar. */
    public static final double SAME_PERSON = 0.5;
    /** A face this similar to another member's face cannot be registered. */
    public static final double DUPLICATE = 0.5;

    private final FaceTemplateRepository templates;
    private final AuditLog audit;

    /**
     * @param templates where the faces are stored
     * @param audit     where the changes are written
     */
    public FaceEnrollmentService(FaceTemplateRepository templates, AuditLog audit) {
        this.templates = templates;
        this.audit = audit;
    }

    /**
     * Registers the face of a member, replacing the previous one.
     *
     * @param member  the member, who must be active and must have accepted the use of the face
     * @param samples the vectors of the pictures taken
     * @param actor   the user who does it, for the audit log
     * @throws BusinessRuleException when the member is not active, there are too few pictures, the
     *                               pictures look like different people or the face belongs to another member
     */
    public void enroll(Member member, List<float[]> samples, String actor) {
        if (!member.isActive()) {
            throw new BusinessRuleException("El socio está inactivo: no se puede registrar su rostro.");
        }
        if (samples.size() < MIN_SAMPLES) {
            throw new BusinessRuleException("Se necesitan al menos " + MIN_SAMPLES + " fotos del rostro.");
        }
        for (int i = 0; i < samples.size(); i++) {
            for (int j = i + 1; j < samples.size(); j++) {
                if (FaceMatcher.cosine(samples.get(i), samples.get(j)) < SAME_PERSON) {
                    throw new BusinessRuleException(
                            "Las fotos no parecen de la misma persona. Repite el registro mirando a la cámara.");
                }
            }
        }
        List<FaceTemplate> others = templates.findAll().stream()
                .filter(t -> t.memberId() != member.getId())
                .toList();
        for (float[] sample : samples) {
            for (FaceTemplate other : others) {
                if (FaceMatcher.cosine(sample, other.vector()) >= DUPLICATE) {
                    throw new BusinessRuleException("Este rostro ya está registrado para otro socio.");
                }
            }
        }
        templates.replace(member.getId(), samples);
        audit.record(actor, AuditAction.FACE_ENROLLED, "Socio " + member.getFullName() + " (" + member.getDocumentNumber() + ")");
    }

    /**
     * Deletes the face of a member.
     *
     * @param member the member
     * @param actor  the user who does it, for the audit log
     */
    public void remove(Member member, String actor) {
        if (templates.has(member.getId())) {
            templates.delete(member.getId());
            audit.record(actor, AuditAction.FACE_REMOVED, "Socio " + member.getFullName() + " (" + member.getDocumentNumber() + ")");
        }
    }

    /**
     * @param memberId the member
     * @return true when the member has a registered face
     */
    public boolean isEnrolled(long memberId) {
        return templates.has(memberId);
    }

    /**
     * Deletes the faces of the members that are inactive or no longer exist.
     *
     * @param members every member of the gym
     * @return how many members lost their face
     */
    public int purgeInactive(List<Member> members) {
        Set<Long> active = members.stream().filter(Member::isActive).map(Member::getId).collect(Collectors.toSet());
        Set<Long> toDelete = templates.findAll().stream().map(FaceTemplate::memberId)
                .filter(id -> !active.contains(id)).collect(Collectors.toSet());
        for (long id : toDelete) {
            templates.delete(id);
            audit.record(com.nextfit.service.audit.AuditService.SYSTEM_USER, AuditAction.FACE_REMOVED, "Socio " + id + " inactivo o inexistente");
        }
        return toDelete.size();
    }
}
