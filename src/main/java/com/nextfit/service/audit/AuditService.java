package com.nextfit.service.audit;

import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.persistence.AuditRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Writes and reads the audit log. It puts the time of every event with the
 * injected clock and never lets a line be changed or removed.
 */
public class AuditService implements AuditLog {

    /** Name written when an event has no user (for example a failed sign-in with an empty name). */
    public static final String SYSTEM_USER = "sistema";

    private final AuditRepository repository;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param repository the audit storage
     * @param clock      the clock that gives the time of each event
     */
    public AuditService(AuditRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void record(String username, AuditAction action, String detail) {
        String who = username == null || username.isBlank() ? SYSTEM_USER : username.trim();
        String what = detail == null ? "" : detail.trim();
        repository.append(new AuditEntry(0, LocalDateTime.now(clock), who, action, what));
    }

    /**
     * Returns every entry, the newest first.
     *
     * @return the entries
     */
    public List<AuditEntry> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(AuditEntry::timestamp).thenComparing(AuditEntry::id).reversed())
                .toList();
    }

    /**
     * Returns the entries of one kind, the newest first.
     *
     * @param action the kind of event
     * @return the entries
     */
    public List<AuditEntry> findByAction(AuditAction action) {
        return findAll().stream()
                .filter(e -> e.action() == action)
                .toList();
    }
}