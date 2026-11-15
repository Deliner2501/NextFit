package com.nextfit.service;

import com.nextfit.model.AuditAction;
import com.nextfit.model.Member;
import com.nextfit.model.MemberCredential;
import com.nextfit.persistence.MemberCredentialRepository;
import com.nextfit.service.audit.AuditLog;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Small fakes shared by the tests of the member passwords. */
final class MemberPasswordTestSupport {

    private MemberPasswordTestSupport() {
    }

    static Member member(long id, String document, boolean active) {
        return new Member(id, "Ana Gómez", document, "3001112233", "a@b.co", LocalDate.of(1998, 5, 1),
                LocalDate.of(2026, 1, 1), "", "Mamá 3001", active);
    }

    static final class MemoryCredentials implements MemberCredentialRepository {
        private final Map<Long, MemberCredential> map = new HashMap<>();

        @Override
        public Optional<MemberCredential> find(long memberId) {
            return Optional.ofNullable(map.get(memberId));
        }

        @Override
        public void save(MemberCredential credential) {
            map.put(credential.memberId(), credential);
        }

        @Override
        public void delete(long memberId) {
            map.remove(memberId);
        }
    }

    static final class RecordingAudit implements AuditLog {
        final List<AuditAction> actions = new ArrayList<>();
        final List<String> details = new ArrayList<>();

        @Override
        public void record(String username, AuditAction action, String detail) {
            actions.add(action);
            details.add(username + "|" + detail);
        }
    }

    static final class MovingClock extends Clock {
        private Instant now = Instant.parse("2026-11-14T15:00:00Z");

        void advanceMinutes(long minutes) {
            now = now.plusSeconds(minutes * 60);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
