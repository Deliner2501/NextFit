package com.nextfit.persistence;

import com.nextfit.model.AuditEntry;

import java.util.List;

/**
 * Storage of the audit log. It only adds and reads: a line that was written
 * can never be changed or deleted from the application.
 */
public interface AuditRepository {

    /**
     * Returns every stored entry, oldest first.
     *
     * @return the entries
     */
    List<AuditEntry> findAll();

    /**
     * Adds a new entry at the end of the log.
     *
     * @param entry the entry to store, with id 0
     * @return the stored entry, with its identifier
     */
    AuditEntry append(AuditEntry entry);
}