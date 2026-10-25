package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AttendanceRecord;

import java.time.LocalDateTime;

/**
 * Converts attendance records to and from CSV rows. It only knows the column
 * layout. Date times are written in ISO format and the exit time is left empty
 * while the member is still inside.
 */
public final class AttendanceCsvMapper {

    /** Header of the attendance file. */
    public static final String[] COLUMNS = {"id", "memberId", "checkIn", "checkOut"};

    private AttendanceCsvMapper() {
    }

    /**
     * Converts an attendance record to a CSV row.
     *
     * @param record the record
     * @return the row
     */
    public static String[] toRow(AttendanceRecord record) {
        return new String[]{
                String.valueOf(record.getId()),
                String.valueOf(record.getMemberId()),
                record.getCheckIn().toString(),
                record.getCheckOut().map(LocalDateTime::toString).orElse("")
        };
    }

    /**
     * Converts a CSV row to an attendance record.
     *
     * @param row the row
     * @return the record
     * @throws PersistenceException when the row is malformed
     */
    public static AttendanceRecord fromRow(String[] row) {
        try {
            boolean hasCheckOut = row.length > 3 && !row[3].isBlank();
            return new AttendanceRecord(
                    Long.parseLong(row[0]),
                    Long.parseLong(row[1]),
                    LocalDateTime.parse(row[2]),
                    hasCheckOut ? LocalDateTime.parse(row[3]) : null);
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de asistencia inválida en el archivo de asistencia", e);
        }
    }
}