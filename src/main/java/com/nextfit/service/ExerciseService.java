package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.persistence.ExerciseRepository;
import com.nextfit.validation.ExerciseValidator;

import java.util.List;

/**
 * Business rules for the exercise catalog. It depends on the repository
 * interface and the validator, never on CSV.
 */
public class ExerciseService {

    private final ExerciseRepository exercises;
    private final ExerciseValidator validator;

    public ExerciseService(ExerciseRepository exercises, ExerciseValidator validator) {
        this.exercises = exercises;
        this.validator = validator;
    }

    /** Returns the whole catalog. */
    public List<Exercise> findAll() {
        return exercises.findAll();
    }

    /** Returns the exercise with the given id, or throws ResourceNotFoundException. */
    public Exercise findById(long id) {
        return exercises.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ejercicio", String.valueOf(id)));
    }

    /**
     * Filters the catalog. Any filter may be null or blank, meaning "do not filter by it".
     *
     * @param text        part of the name (case-insensitive)
     * @param muscleGroup muscle group to keep, or null for all
     * @param difficulty  difficulty to keep, or null for all
     */
    public List<Exercise> search(String text, MuscleGroup muscleGroup, Difficulty difficulty) {
        String wanted = text == null ? "" : text.trim().toLowerCase();
        return exercises.findAll().stream()
                .filter(e -> e.getName().toLowerCase().contains(wanted))
                .filter(e -> muscleGroup == null || e.getMuscleGroup() == muscleGroup)
                .filter(e -> difficulty == null || e.getDifficulty() == difficulty)
                .toList();
    }

    /** Validates and stores a new exercise; the name must be unused. */
    public Exercise create(Exercise exercise) {
        validator.validate(exercise);
        ensureNameIsFree(exercise);
        // withId(0) fuerza a que el repositorio asigne un id nuevo.
        return exercises.save(exercise.withId(0));
    }

    /** Validates and stores the changes of an existing exercise. */
    public Exercise update(Exercise exercise) {
        findById(exercise.getId());
        validator.validate(exercise);
        ensureNameIsFree(exercise);
        return exercises.save(exercise);
    }

    // Regla: no puede haber dos ejercicios con el mismo nombre (sin importar mayúsculas).
    private void ensureNameIsFree(Exercise exercise) {
        boolean usedByAnother = exercises.findByName(exercise.getName().trim())
                .filter(other -> other.getId() != exercise.getId())
                .isPresent();
        if (usedByAnother) {
            throw new BusinessRuleException(
                    "Ya existe un ejercicio llamado \"" + exercise.getName().trim() + "\"");
        }
    }
}