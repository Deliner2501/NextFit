package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;

/**
 * Converts exercises to and from CSV rows. It only knows the column layout.
 */
public final class ExerciseCsvMapper {

    /** Header of the exercises file. */
    public static final String[] COLUMNS = {"id", "name", "description", "muscleGroup", "difficulty", "imagePath"};

    private ExerciseCsvMapper() {
    }

    /**
     * Converts an exercise to a CSV row.
     *
     * @param exercise the exercise
     * @return the row
     */
    public static String[] toRow(Exercise exercise) {
        // EXPLICACIÓN: cada atributo se convierte a texto, en el mismo orden
        // que las columnas de COLUMNS. Los enums se guardan por su nombre
        // interno (por ejemplo "CHEST"), no por la etiqueta en español.
        return new String[]{
                String.valueOf(exercise.getId()),
                exercise.getName(),
                exercise.getDescription(),
                exercise.getMuscleGroup().name(),
                exercise.getDifficulty().name(),
                exercise.getImagePath()
        };
    }

    /**
     * Converts a CSV row to an exercise.
     *
     * @param row the row
     * @return the exercise
     * @throws PersistenceException when the row is malformed
     */
    public static Exercise fromRow(String[] row) {
        // EXPLICACIÓN: es el camino inverso. Si una fila está dañada (le faltan
        // columnas o el grupo muscular no existe), se lanza una
        // PersistenceException clara en lugar de dejar que el programa se caiga.
        try {
            return new Exercise(
                    Long.parseLong(row[0]),
                    row[1],
                    row[2],
                    MuscleGroup.valueOf(row[3]),
                    Difficulty.valueOf(row[4]),
                    row[5]);
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de ejercicio inválida en el archivo de ejercicios", e);
        }
    }
}