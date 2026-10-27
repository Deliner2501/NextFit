package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.RoutineAssignment;

import java.time.LocalDate;

/** Converts a {@link RoutineAssignment} to and from a CSV row. */
public final class AssignmentCsvMapper {

    /** Column names of the assignments file, in order. */
    public static final String[] COLUMNS = {"id", "memberId", "routineId", "assignedAt", "status"};

    private AssignmentCsvMapper() {
    }

    /** Turns an assignment into the text values of one CSV row. */
    public static String[] toRow(RoutineAssignment assignment) {
        return new String[]{
                String.valueOf(assignment.getId()),
                String.valueOf(assignment.getMemberId()),
                String.valueOf(assignment.getRoutineId()),
                assignment.getAssignedAt().toString(),
                assignment.getStatus().name()   // se guarda el nombre del enum, p. ej. ACTIVE
        };
    }

    /** Builds an assignment from a CSV row; a damaged row becomes a PersistenceException. */
    public static RoutineAssignment fromRow(String[] row) {
        try {
            return new RoutineAssignment(
                    Long.parseLong(row[0]),
                    Long.parseLong(row[1]),
                    Long.parseLong(row[2]),
                    LocalDate.parse(row[3]),
                    AssignmentStatus.valueOf(row[4]));
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de asignación inválida en el archivo de asignaciones", e);
        }
    }
}