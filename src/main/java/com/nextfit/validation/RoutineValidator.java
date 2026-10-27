package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;

import java.time.LocalDate;

/**
 * Validates a routine, its days and its exercises before it is stored. It has
 * no access to files or screens, so it can be tested on its own.
 */
public class RoutineValidator {

    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 60;
    private static final int OBJECTIVE_MAX = 100;
    private static final int SETS_MAX = 10;
    private static final int REPETITIONS_MAX = 100;
    private static final int REST_MAX_SECONDS = 600;

    /**
     * Validates the whole routine: its own data, then every day, then every
     * exercise of every day.
     *
     * @param routine the routine to check
     * @throws InvalidDataException when any part breaks its rule
     */
    public void validate(Routine routine) {
        validateName(routine.getName());
        validateObjective(routine.getObjective());
        validateTrainerId(routine.getTrainerId());
        validateCreatedAt(routine.getCreatedAt());
        // EXPLICACIÓN: una rutina vacía no sirve, así que se exige al menos un día.
        if (routine.getDays().isEmpty()) {
            throw new InvalidDataException("días", "la rutina debe tener al menos un día");
        }
        for (RoutineDay day : routine.getDays()) {
            validateDay(day);
        }
    }

    /**
     * Validates the routine name: it must exist and have from 3 to 60 characters.
     *
     * @param name the routine name
     * @throws InvalidDataException when it is empty or out of range
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDataException("nombre de la rutina", "no puede estar vacío");
        }
        if (name.trim().length() < NAME_MIN || name.length() > NAME_MAX) {
            throw new InvalidDataException("nombre de la rutina",
                    "debe tener entre " + NAME_MIN + " y " + NAME_MAX + " caracteres");
        }
    }

    /**
     * Validates the objective: it must exist and not be too long.
     *
     * @param objective the goal of the routine
     * @throws InvalidDataException when it is empty or too long
     */
    public void validateObjective(String objective) {
        if (objective == null || objective.isBlank()) {
            throw new InvalidDataException("objetivo", "no puede estar vacío");
        }
        if (objective.length() > OBJECTIVE_MAX) {
            throw new InvalidDataException("objetivo",
                    "no puede superar los " + OBJECTIVE_MAX + " caracteres");
        }
    }

    /**
     * Validates that the routine has a trainer.
     *
     * @param trainerId the identifier of the trainer
     * @throws InvalidDataException when it is not a valid identifier
     */
    public void validateTrainerId(long trainerId) {
        if (trainerId <= 0) {
            throw new InvalidDataException("entrenador", "debe seleccionar un entrenador");
        }
    }

    /**
     * Validates that the creation date exists.
     *
     * @param createdAt the creation date
     * @throws InvalidDataException when it is null
     */
    public void validateCreatedAt(LocalDate createdAt) {
        if (createdAt == null) {
            throw new InvalidDataException("fecha de creación", "no puede estar vacía");
        }
    }

    /**
     * Validates a day: it needs a name and at least one exercise, and each
     * exercise must be valid.
     *
     * @param day the day to check
     * @throws InvalidDataException when the day or one of its exercises is invalid
     */
    public void validateDay(RoutineDay day) {
        if (day.getName() == null || day.getName().isBlank()) {
            throw new InvalidDataException("nombre del día", "no puede estar vacío");
        }
        if (day.getExercises().isEmpty()) {
            throw new InvalidDataException("ejercicios",
                    "el día '" + day.getName() + "' debe tener al menos un ejercicio");
        }
        for (RoutineExercise exercise : day.getExercises()) {
            validateRoutineExercise(exercise);
        }
    }

    /**
     * Validates one exercise line: a catalog exercise, sets from 1 to 10,
     * repetitions from 1 to 100 and a rest from 0 to 600 seconds.
     *
     * @param exercise the exercise line to check
     * @throws InvalidDataException when any number is out of range
     */
    public void validateRoutineExercise(RoutineExercise exercise) {
        if (exercise.getExerciseId() <= 0) {
            throw new InvalidDataException("ejercicio", "debe seleccionar un ejercicio del catálogo");
        }
        if (exercise.getSets() < 1 || exercise.getSets() > SETS_MAX) {
            throw new InvalidDataException("series", "deben estar entre 1 y " + SETS_MAX);
        }
        if (exercise.getRepetitions() < 1 || exercise.getRepetitions() > REPETITIONS_MAX) {
            throw new InvalidDataException("repeticiones", "deben estar entre 1 y " + REPETITIONS_MAX);
        }
        if (exercise.getRestSeconds() < 0 || exercise.getRestSeconds() > REST_MAX_SECONDS) {
            throw new InvalidDataException("descanso", "debe estar entre 0 y " + REST_MAX_SECONDS + " segundos");
        }
    }
}