package com.nextfit.service.audit;

import com.nextfit.model.AuditAction;

/**
 * What the other services need from the audit log: to write an event. They
 * depend on this small interface, so they do not know how or where it is stored.
 */
public interface AuditLog {

    /** An audit log that writes nothing, for code and tests that do not audit. */
    AuditLog NONE = (username, action, detail) -> {
    };

    /**
     * Writes one event.
     *
     * @param username the sign-in name of whoever did it
     * @param action   the kind of event
     * @param detail   a short text with the affected data, never a password
     */
    void record(String username, AuditAction action, String detail);
}