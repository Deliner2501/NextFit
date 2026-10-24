package com.nextfit.model;

/**
 * One exercise inside a routine day, with the numbers that apply to it:
 * sets, repetitions and rest. The class is immutable.
 */
public class RoutineExercise {

    // EXPLICACIÓN: esta clase es "un ejercicio dentro de un día de la rutina".
    // No copia el ejercicio completo: solo guarda su id (exerciseId) y lo que
    // cambia en cada rutina: series, repeticiones y descanso. Cuando una pantalla
    // necesita el nombre o la imagen, busca el ejercicio en el catálogo con ese id.
    private final long exerciseId;
    private final int sets;
    private final int repetitions;
    private final int restSeconds;

    /**
     * Creates a routine exercise.
     *
     * @param exerciseId  the identifier of the catalog exercise
     * @param sets        the number of sets
     * @param repetitions the repetitions per set
     * @param restSeconds the rest between sets, in seconds
     */
    public RoutineExercise(long exerciseId, int sets, int repetitions, int restSeconds) {
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.repetitions = repetitions;
        this.restSeconds = restSeconds;
    }

    public long getExerciseId() {
        return exerciseId;
    }

    public int getSets() {
        return sets;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public int getRestSeconds() {
        return restSeconds;
    }
}