package com.nextfit.persistence;

import com.nextfit.model.Exercise;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores the exercise catalog in a CSV file through {@link CsvStore}.
 */
public class CsvExerciseRepository implements ExerciseRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvExerciseRepository(Path file) {
        // EXPLICACIÓN: CsvStore (de Deiner) es el único que sabe leer y escribir
        // el archivo. Este repositorio solo le pasa las filas y convierte cada
        // fila en un Exercise con ExerciseCsvMapper.
        this.store = new CsvStore(file, ExerciseCsvMapper.COLUMNS);
    }

    @Override
    public List<Exercise> findAll() {
        List<Exercise> exercises = new ArrayList<>();
        for (String[] row : store.readAll()) {
            exercises.add(ExerciseCsvMapper.fromRow(row));
        }
        return exercises;
    }

    @Override
    public Optional<Exercise> findById(long id) {
        return findAll().stream().filter(e -> e.getId() == id).findFirst();
    }

    @Override
    public Optional<Exercise> findByName(String name) {
        return findAll().stream()
                .filter(e -> e.getName().trim().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    @Override
    public Exercise save(Exercise exercise) {
        List<Exercise> exercises = findAll();
        Exercise toStore = exercise;
        if (exercise.getId() == 0) {
            // EXPLICACIÓN: ejercicio nuevo. Su id es el mayor que existe más 1,
            // y withId devuelve la copia con ese número.
            long nextId = exercises.stream().mapToLong(Exercise::getId).max().orElse(0) + 1;
            toStore = exercise.withId(nextId);
            exercises.add(toStore);
        } else {
            // EXPLICACIÓN: ejercicio que ya existía. Se reemplaza el que tiene el mismo id.
            for (int i = 0; i < exercises.size(); i++) {
                if (exercises.get(i).getId() == exercise.getId()) {
                    exercises.set(i, exercise);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Exercise e : exercises) {
            rows.add(ExerciseCsvMapper.toRow(e));
        }
        store.writeAll(rows);
        return toStore;
    }
}