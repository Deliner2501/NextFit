package com.nextfit.persistence;

import com.nextfit.model.Routine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores routines in two CSV files through {@link CsvStore}: one with the
 * routines and one with the exercise lines of their days.
 */
public class CsvRoutineRepository implements RoutineRepository {

    private final CsvStore routineStore;
    private final CsvStore lineStore;

    /**
     * Opens the repository on the given files, creating them when they do not exist.
     *
     * @param routinesFile the CSV file with the routines
     * @param linesFile    the CSV file with the exercise lines of the routines
     */
    public CsvRoutineRepository(Path routinesFile, Path linesFile) {
        this.routineStore = new CsvStore(routinesFile, RoutineCsvMapper.ROUTINE_COLUMNS);
        this.lineStore = new CsvStore(linesFile, RoutineCsvMapper.LINE_COLUMNS);
    }

    @Override
    public List<Routine> findAll() {
        // EXPLICACIÓN: primero se leen todas las líneas de ejercicios y se
        // agrupan por id de rutina; luego cada fila de rutina se une con sus líneas.
        Map<Long, List<String[]>> linesByRoutine = new HashMap<>();
        for (String[] row : lineStore.readAll()) {
            linesByRoutine.computeIfAbsent(Long.parseLong(row[0]), k -> new ArrayList<>()).add(row);
        }
        List<Routine> routines = new ArrayList<>();
        for (String[] row : routineStore.readAll()) {
            long id = Long.parseLong(row[0]);
            routines.add(RoutineCsvMapper.fromRows(row, linesByRoutine.getOrDefault(id, List.of())));
        }
        return routines;
    }

    @Override
    public Optional<Routine> findById(long id) {
        return findAll().stream().filter(r -> r.getId() == id).findFirst();
    }

    @Override
    public List<Routine> findByTrainerId(long trainerId) {
        List<Routine> result = new ArrayList<>();
        for (Routine routine : findAll()) {
            if (routine.getTrainerId() == trainerId) {
                result.add(routine);
            }
        }
        return result;
    }

    @Override
    public Routine save(Routine routine) {
        List<Routine> routines = findAll();
        Routine toStore = routine;
        if (routine.getId() == 0) {
            long nextId = routines.stream().mapToLong(Routine::getId).max().orElse(0) + 1;
            toStore = routine.withId(nextId);
            routines.add(toStore);
        } else {
            for (int i = 0; i < routines.size(); i++) {
                if (routines.get(i).getId() == routine.getId()) {
                    routines.set(i, routine);
                    break;
                }
            }
        }
        // EXPLICACIÓN: se reescriben los dos archivos completos con el estado final.
        List<String[]> routineRows = new ArrayList<>();
        List<String[]> lineRows = new ArrayList<>();
        for (Routine r : routines) {
            routineRows.add(RoutineCsvMapper.toRoutineRow(r));
            lineRows.addAll(RoutineCsvMapper.toLineRows(r));
        }
        routineStore.writeAll(routineRows);
        lineStore.writeAll(lineRows);
        return toStore;
    }
}