package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;

/**
 * Validates the data of a catalog exercise before it is stored. It has no
 * access to files or screens, so it can be tested on its own.
 */
public class ExerciseValidator {

    // EXPLICACIÓN: los límites van como constantes con nombre, no como números
    // sueltos dentro del código.
    private static final int NAME_MIN = 3;
    private static final int NAME_MAX = 60;
    private static final int DESCRIPTION_MAX = 300;

    /**
     * Validates every field of an exercise.
     *
     * @param exercise the exercise to check
     * @throws InvalidDataException when any field breaks its rule
     */
    public void validate(Exercise exercise) {
        // EXPLICACIÓN: este método solo llama a las reglas una por una. Si
        // alguna falla, lanza InvalidDataException y el resto ya no se revisa.
        validateName(exercise.getName());
        validateDescription(exercise.getDescription());
        validateMuscleGroup(exercise.getMuscleGroup());
        validateDifficulty(exercise.getDifficulty());
    }

    /**
     * Validates the name: it must exist and have from 3 to 60 characters.
     *
     * @param name the exercise name
     * @throws InvalidDataException when it is empty or out of range
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidDataException("nombre del ejercicio", "no puede estar vacío");
        }
        if (name.trim().length() < NAME_MIN || name.length() > NAME_MAX) {
            throw new InvalidDataException("nombre del ejercicio",
                    "debe tener entre " + NAME_MIN + " y " + NAME_MAX + " caracteres");
        }
    }

    /**
     * Validates the description. It is optional, but it cannot be too long.
     *
     * @param description the description, or an empty text
     * @throws InvalidDataException when it is longer than the limit
     */
    public void validateDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX) {
            throw new InvalidDataException("descripción",
                    "no puede superar los " + DESCRIPTION_MAX + " caracteres");
        }
    }

    /**
     * Validates that a muscle group was chosen.
     *
     * @param muscleGroup the muscle group
     * @throws InvalidDataException when it is null
     */
    public void validateMuscleGroup(MuscleGroup muscleGroup) {
        if (muscleGroup == null) {
            throw new InvalidDataException("grupo muscular", "debe seleccionar un grupo muscular");
        }
    }

    /**
     * Validates that a difficulty was chosen.
     *
     * @param difficulty the difficulty level
     * @throws InvalidDataException when it is null
     */
    public void validateDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            throw new InvalidDataException("dificultad", "debe seleccionar una dificultad");
        }
    }
}