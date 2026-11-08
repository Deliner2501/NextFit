package com.nextfit.service.audit;

import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.persistence.AuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditServiceTest {

    private MovableClock clock;
    private InMemoryAudit repository;
    private AuditService service;

    @BeforeEach
    void setUp() {
        clock = new MovableClock(Instant.parse("2026-11-10T15:00:00Z"));
        repository = new InMemoryAudit();
        service = new AuditService(repository, clock);
    }

    @Test
    void recordStoresTheEventWithTheTimeOfTheClock() {
        service.record("admin", AuditAction.LOGIN, "entró");

        AuditEntry entry = repository.findAll().get(0);
        assertEquals(LocalDateTime.of(2026, 11, 10, 15, 0), entry.timestamp());
        assertEquals("admin", entry.username());
        assertEquals(AuditAction.LOGIN, entry.action());
        assertEquals("entró", entry.detail());
    }

    @Test
    void recordTrimsTheNameAndTheDetail() {
        service.record("  admin  ", AuditAction.LOGIN, "  entró  ");

        AuditEntry entry = repository.findAll().get(0);
        assertEquals("admin", entry.username());
        assertEquals("entró", entry.detail());
    }

    @Test
    void recordUsesTheSystemUserWhenThereIsNoName() {
        service.record(null, AuditAction.LOGIN_FAILED, "");
        service.record("   ", AuditAction.LOGIN_FAILED, "");

        assertEquals(AuditService.SYSTEM_USER, repository.findAll().get(0).username());
        assertEquals(AuditService.SYSTEM_USER, repository.findAll().get(1).username());
    }

    @Test
    void recordAcceptsANullDetail() {
        service.record("admin", AuditAction.LOGOUT, null);

        assertEquals("", repository.findAll().get(0).detail());
    }

    @Test
    void findAllReturnsTheNewestFirst() {
        service.record("admin", AuditAction.LOGIN, "primero");
        clock.advance(Duration.ofMinutes(5));
        service.record("admin", AuditAction.LOGOUT, "segundo");
        clock.advance(Duration.ofMinutes(5));
        service.record("recepcion", AuditAction.LOGIN, "tercero");

        List<AuditEntry> entries = service.findAll();

        assertEquals("tercero", entries.get(0).detail());
        assertEquals("segundo", entries.get(1).detail());
        assertEquals("primero", entries.get(2).detail());
    }

    @Test
    void findAllBreaksTiesByIdWhenTheTimeIsTheSame() {
        service.record("admin", AuditAction.LOGIN, "a");
        service.record("admin", AuditAction.LOGOUT, "b");

        List<AuditEntry> entries = service.findAll();

        assertEquals("b", entries.get(0).detail());
        assertEquals("a", entries.get(1).detail());
    }

    @Test
    void findByActionKeepsOnlyThatKind() {
        service.record("admin", AuditAction.LOGIN, "");
        service.record("admin", AuditAction.LOGIN_FAILED, "");
        service.record("recepcion", AuditAction.LOGIN, "");

        List<AuditEntry> logins = service.findByAction(AuditAction.LOGIN);

        assertEquals(2, logins.size());
        assertTrue(logins.stream().allMatch(e -> e.action() == AuditAction.LOGIN));
    }

    @Test
    void theNoneLogWritesNothingAndDoesNotFail() {
        assertDoesNotThrow(() -> AuditLog.NONE.record("admin", AuditAction.LOGIN, "x"));
    }

    /** Keeps the entries in a list, like a real repository but without a file. */
    private static final class InMemoryAudit implements AuditRepository {

        private final List<AuditEntry> entries = new ArrayList<>();

        @Override
        public List<AuditEntry> findAll() {
            return List.copyOf(entries);
        }

        @Override
        public AuditEntry append(AuditEntry entry) {
            AuditEntry stored = entry.withId(entries.size() + 1L);
            entries.add(stored);
            return stored;
        }
    }

    /** A clock that only moves when the test says so. */
    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration time) {
            now = now.plus(time);
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