package com.nextfit.service;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.persistence.AttendanceRepository;
import com.nextfit.persistence.HealthProfileRepository;
import com.nextfit.persistence.MeasurementRepository;
import com.nextfit.persistence.MemberRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory repositories used by the service tests, so no file is touched.
 */
final class FakeRepositories {

    private FakeRepositories() {
    }

    /** Builds a valid active member with the given document number. */
    static Member newMember(String documentNumber) {
        return new Member(0, "Ana Pérez", documentNumber, "3001234567", "ana@example.com",
                LocalDate.of(2000, 5, 10), LocalDate.of(2026, 10, 1), "", "Mamá 3110000000", true);
    }

    static class FakeMemberRepository implements MemberRepository {
        private final List<Member> stored = new ArrayList<>();

        @Override
        public List<Member> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public Optional<Member> findById(long id) {
            return stored.stream().filter(m -> m.getId() == id).findFirst();
        }

        @Override
        public Optional<Member> findByDocumentNumber(String documentNumber) {
            return stored.stream().filter(m -> m.getDocumentNumber().equals(documentNumber)).findFirst();
        }

        @Override
        public Member save(Member member) {
            Member toStore = member.getId() == 0 ? member.withId(stored.size() + 1L) : member;
            stored.removeIf(m -> m.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }

    static class FakeAttendanceRepository implements AttendanceRepository {
        private final List<AttendanceRecord> stored = new ArrayList<>();

        @Override
        public List<AttendanceRecord> findAll() {
            return new ArrayList<>(stored);
        }

        @Override
        public List<AttendanceRecord> findByMemberId(long memberId) {
            return stored.stream().filter(r -> r.getMemberId() == memberId).toList();
        }

        @Override
        public Optional<AttendanceRecord> findOpenByMemberId(long memberId) {
            return stored.stream().filter(r -> r.getMemberId() == memberId && r.isOpen()).findFirst();
        }

        @Override
        public AttendanceRecord save(AttendanceRecord record) {
            AttendanceRecord toStore = record.getId() == 0 ? record.withId(stored.size() + 1L) : record;
            stored.removeIf(r -> r.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }

    static class FakeHealthProfileRepository implements HealthProfileRepository {
        private final List<HealthProfile> stored = new ArrayList<>();

        @Override
        public Optional<HealthProfile> findByMemberId(long memberId) {
            return stored.stream().filter(p -> p.getMemberId() == memberId).findFirst();
        }

        @Override
        public HealthProfile save(HealthProfile profile) {
            stored.removeIf(p -> p.getMemberId() == profile.getMemberId());
            stored.add(profile);
            return profile;
        }
    }

    static class FakeMeasurementRepository implements MeasurementRepository {
        private final List<Measurement> stored = new ArrayList<>();

        @Override
        public List<Measurement> findByMemberId(long memberId) {
            return stored.stream()
                    .filter(m -> m.getMemberId() == memberId)
                    .sorted(java.util.Comparator.comparing(Measurement::getDate))
                    .toList();
        }

        @Override
        public Measurement save(Measurement measurement) {
            Measurement toStore = measurement.getId() == 0 ? measurement.withId(stored.size() + 1L) : measurement;
            stored.removeIf(m -> m.getId() == toStore.getId());
            stored.add(toStore);
            return toStore;
        }
    }
}