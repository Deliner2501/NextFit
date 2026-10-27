package com.nextfit.persistence;

import com.nextfit.model.AttendanceRecord;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Stores attendance records in a CSV file through {@link CsvStore}.
 */
public class CsvAttendanceRepository implements AttendanceRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvAttendanceRepository(Path file) {
        this.store = new CsvStore(file, AttendanceCsvMapper.COLUMNS);
    }

    @Override
    public List<AttendanceRecord> findAll() {
        List<AttendanceRecord> records = new ArrayList<>();
        for (String[] row : store.readAll()) {
            records.add(AttendanceCsvMapper.fromRow(row));
        }
        return records;
    }

    @Override
    public List<AttendanceRecord> findByMemberId(long memberId) {
        return findAll().stream()
                .filter(r -> r.getMemberId() == memberId)
                .sorted(Comparator.comparing(AttendanceRecord::getCheckIn))
                .toList();
    }

    @Override
    public Optional<AttendanceRecord> findOpenByMemberId(long memberId) {
        return findAll().stream()
                .filter(r -> r.getMemberId() == memberId && r.isOpen())
                .findFirst();
    }

    @Override
    public AttendanceRecord save(AttendanceRecord record) {
        List<AttendanceRecord> records = findAll();
        AttendanceRecord toStore = record;
        if (record.getId() == 0) {
            long nextId = records.stream().mapToLong(AttendanceRecord::getId).max().orElse(0) + 1;
            toStore = record.withId(nextId);
            records.add(toStore);
        } else {
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).getId() == record.getId()) {
                    records.set(i, record);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (AttendanceRecord r : records) {
            rows.add(AttendanceCsvMapper.toRow(r));
        }
        store.writeAll(rows);
        return toStore;
    }
}