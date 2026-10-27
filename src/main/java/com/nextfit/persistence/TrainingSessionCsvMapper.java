package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.TrainingSession;

import java.time.LocalDate;

/** Converts a {@link TrainingSession} to and from a CSV row. */
public final class TrainingSessionCsvMapper {

    /** Column names of the sessions file, in order. */
    public static final String[] COLUMNS = {"id", "memberId", "assignmentId", "dayName",
            "date", "durationMinutes", "notes"};

    private TrainingSessionCsvMapper() {
    }

    /** Turns a session into the text values of one CSV row. */
    public static String[] toRow(TrainingSession session) {
        return new String[]{
                String.valueOf(session.getId()),
                String.valueOf(session.getMemberId()),
                String.valueOf(session.getAssignmentId()),
                session.getDayName(),
                session.getDate().toString(),
                String.valueOf(session.getDurationMinutes()),
                session.getNotes()
        };
    }

    /** Builds a session from a CSV row; a damaged row becomes a PersistenceException. */
    public static TrainingSession fromRow(String[] row) {
        try {
            return new TrainingSession(
                    Long.parseLong(row[0]),
                    Long.parseLong(row[1]),
                    Long.parseLong(row[2]),
                    row[3],
                    LocalDate.parse(row[4]),
                    Integer.parseInt(row[5]),
                    row[6]);
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de sesión inválida en el archivo de sesiones", e);
        }
    }
}