package com.nextfit.service.face;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.FaceTemplate;
import com.nextfit.model.Member;
import com.nextfit.persistence.FaceTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.nextfit.service.face.FaceTestData.face;
import static com.nextfit.service.face.FaceTestData.member;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceEnrollmentServiceTest {

    /** Memory storage for the tests. */
    static final class MemoryTemplates implements FaceTemplateRepository {
        final List<FaceTemplate> stored = new ArrayList<>();

        @Override
        public List<FaceTemplate> findAll() {
            return List.copyOf(stored);
        }

        @Override
        public void replace(long memberId, List<float[]> vectors) {
            delete(memberId);
            vectors.forEach(v -> stored.add(new FaceTemplate(memberId, v)));
        }

        @Override
        public void delete(long memberId) {
            stored.removeIf(t -> t.memberId() == memberId);
        }
    }

    private MemoryTemplates repo;
    private List<String> audit;
    private FaceEnrollmentService service;
    private final List<float[]> anaFaces = List.of(face(1, 0, 0), face(0.95, 0.1, 0), face(0.9, 0.05, 0.1));

    @BeforeEach
    void setUp() {
        repo = new MemoryTemplates();
        audit = new ArrayList<>();
        service = new FaceEnrollmentService(repo, (user, action, detail) -> audit.add(user + "|" + action + "|" + detail));
    }

    @Test
    void storesTheFaceAndWritesTheAudit() {
        Member ana = member(1, "Ana Pérez", true);

        service.enroll(ana, anaFaces, "admin");

        assertEquals(3, repo.findAll().size());
        assertTrue(service.isEnrolled(1));
        assertEquals(1, audit.size());
        assertTrue(audit.get(0).contains(AuditAction.FACE_ENROLLED.name()));
    }

    @Test
    void needsAtLeastThreePictures() {
        assertThrows(BusinessRuleException.class,
                () -> service.enroll(member(1, "Ana", true), anaFaces.subList(0, 2), "admin"));
        assertFalse(service.isEnrolled(1));
    }

    @Test
    void picturesOfDifferentPeopleAreRefused() {
        List<float[]> mixed = List.of(face(1, 0, 0), face(0.9, 0.1, 0), face(0, 1, 0));
        assertThrows(BusinessRuleException.class, () -> service.enroll(member(1, "Ana", true), mixed, "admin"));
    }

    @Test
    void anInactiveMemberCannotBeRegistered() {
        assertThrows(BusinessRuleException.class, () -> service.enroll(member(1, "Ana", false), anaFaces, "admin"));
    }

    @Test
    void aFaceThatBelongsToAnotherMemberIsRefused() {
        service.enroll(member(1, "Ana", true), anaFaces, "admin");

        assertThrows(BusinessRuleException.class, () -> service.enroll(member(2, "Luis", true), anaFaces, "admin"));
        assertFalse(service.isEnrolled(2));
    }

    @Test
    void registeringAgainReplacesTheOldFace() {
        Member ana = member(1, "Ana", true);
        service.enroll(ana, anaFaces, "admin");
        service.enroll(ana, anaFaces, "admin");

        assertEquals(3, repo.findAll().size());
    }

    @Test
    void removeDeletesAndAudits() {
        Member ana = member(1, "Ana", true);
        service.enroll(ana, anaFaces, "admin");

        service.remove(ana, "admin");

        assertFalse(service.isEnrolled(1));
        assertEquals(2, audit.size());
        assertTrue(audit.get(1).contains(AuditAction.FACE_REMOVED.name()));
        service.remove(ana, "admin");
        assertEquals(2, audit.size());
    }

    @Test
    void purgeDeletesTheFacesOfInactiveOrMissingMembers() {
        Member ana = member(1, "Ana", true);
        service.enroll(ana, anaFaces, "admin");
        repo.replace(2, List.of(face(0, 1, 0)));
        repo.replace(3, List.of(face(0, 0, 1)));

        int purged = service.purgeInactive(List.of(ana, member(2, "Luis", false)));

        assertEquals(2, purged);
        assertTrue(service.isEnrolled(1));
        assertFalse(service.isEnrolled(2));
        assertFalse(service.isEnrolled(3));
    }
}
