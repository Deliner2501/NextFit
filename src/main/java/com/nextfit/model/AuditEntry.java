package com.nextfit.model;

import java.time.LocalDateTime;

/**
 * One line of the audit log: who did what and when. It is immutable, because
 * a log line must never change once it is written.
 *
 * @param id        the identifier, 0 while it is not stored yet
 * @param timestamp the moment of the event
 * @param username  the sign-in name of whoever did it
 * @param action    the kind of event
 * @param detail    a short text with the affected data (never a password)
 */
public record AuditEntry(long id, LocalDateTime timestamp, String username,
                         AuditAction action, String detail) {

    /**
     * Returns a copy with the given identifier.
     *
     * @param newId the identifier
     * @return the copy
     */
    public AuditEntry withId(long newId) {
        return new AuditEntry(newId, timestamp, username, action, detail);
    }
}