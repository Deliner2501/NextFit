package com.nextfit.persistence;

import com.nextfit.model.RoutineAssignment;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CSV implementation of {@link AssignmentRepository}. */
public class CsvAssignmentRepository implements AssignmentRepository {

    private final CsvStore store;

    /** @param file the CSV file where assignments are stored */
    public CsvAssignmentRepository(Path file) {
        this.store = new CsvStore(file, AssignmentCsvMapper.COLUMNS);
    }

    @Override
    public List<RoutineAssignment> findAll() {
        List<RoutineAssignment> assignments = new ArrayList<>();
        for (String[] row : store.readAll()) {
            assignments.add(AssignmentCsvMapper.fromRow(row));
        }
        return assignments;
    }

    @Override
    public Optional<RoutineAssignment> findById(long id) {
        return findAll().stream().filter(a -> a.getId() == id).findFirst();
    }

    @Override
    public List<RoutineAssignment> findByMemberId(long memberId) {
        return findAll().stream().filter(a -> a.getMemberId() == memberId).toList();
    }

    @Override
    public RoutineAssignment save(RoutineAssignment assignment) {
        List<RoutineAssignment> assignments = findAll();
        RoutineAssignment toStore = assignment;
        if (assignment.getId() == 0) {
            // Id nuevo: el mayor existente + 1.
            long nextId = assignments.stream().mapToLong(RoutineAssignment::getId).max().orElse(0) + 1;
            toStore = assignment.withId(nextId);
            assignments.add(toStore);
        } else {
            // Id existente: se reemplaza la asignación con ese id.
            for (int i = 0; i < assignments.size(); i++) {
                if (assignments.get(i).getId() == assignment.getId()) {
                    assignments.set(i, assignment);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (RoutineAssignment a : assignments) {
            rows.add(AssignmentCsvMapper.toRow(a));
        }
        store.writeAll(rows);
        return toStore;
    }
}