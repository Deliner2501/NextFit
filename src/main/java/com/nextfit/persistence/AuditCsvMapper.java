package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;

import java.time.LocalDateTime;

/**
 * Converts audit entries to and from CSV rows. It only knows the column
 * layout. The moment is written in ISO format and the action by its name.
 */
public final class AuditCsvMapper {

    /** Header of the audit file. */
    public static final String[] COLUMNS = {"id", "timestamp", "username", "action", "detail"};

    private AuditCsvMapper() {
    }

    /**
     * Converts an audit entry to a CSV row.
     *
     * @param entry the entry
     * @return the row
     */
    public static String[] toRow(AuditEntry entry) {
        return new String[]{
                String.valueOf(entry.id()),
                entry.timestamp().toString(),
                entry.username(),
                entry.action().name(),
                entry.detail()
        };
    }

    /**
     * Converts a CSV row to an audit entry.
     *
     * @param row the row
     * @return the entry
     * @throws PersistenceException when the row is malformed
     */
    public static AuditEntry fromRow(String[] row) {
        try {
            return new AuditEntry(
                    Long.parseLong(row[0]),
                    LocalDateTime.parse(row[1]),
                    row[2],
                    AuditAction.valueOf(row[3]),
                    row.length > 4 ? row[4] : "");
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de auditoría inválida en el archivo de auditoría", e);
        }
    }
}