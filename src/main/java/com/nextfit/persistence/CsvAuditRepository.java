package com.nextfit.persistence;

import com.nextfit.model.AuditEntry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the audit log in a CSV file through {@link CsvStore}.
 */
public class CsvAuditRepository implements AuditRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvAuditRepository(Path file) {
        this.store = new CsvStore(file, AuditCsvMapper.COLUMNS);
    }

    @Override
    public synchronized List<AuditEntry> findAll() {
        List<AuditEntry> entries = new ArrayList<>();
        for (String[] row : store.readAll()) {
            entries.add(AuditCsvMapper.fromRow(row));
        }
        return entries;
    }

    @Override
    public synchronized AuditEntry append(AuditEntry entry) {
        List<AuditEntry> entries = findAll();
        long nextId = entries.stream().mapToLong(AuditEntry::id).max().orElse(0) + 1;
        AuditEntry toStore = entry.withId(nextId);
        entries.add(toStore);
        List<String[]> rows = new ArrayList<>();
        for (AuditEntry e : entries) {
            rows.add(AuditCsvMapper.toRow(e));
        }
        store.writeAll(rows);
        return toStore;
    }
}