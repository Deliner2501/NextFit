package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.persistence.RoutineRepository;
import com.nextfit.validation.RoutineValidator;

import java.util.List;

/**
 * Business rules for routines. The validator checks the shape of the data;
 * this service checks that what the routine points to really exists.
 */
public class RoutineService {

    private final RoutineRepository routines;
    private final RoutineValidator validator;
    private final TrainerService trainers;
    private final ExerciseService exercises;

    public RoutineService(RoutineRepository routines, RoutineValidator validator,
                          TrainerService trainers, ExerciseService exercises) {
        this.routines = routines;
        this.validator = validator;
        this.trainers = trainers;
        this.exercises = exercises;
    }

    /** Returns all routines. */
    public List<Routine> findAll() {
        return routines.findAll();
    }

    /** Returns the routine with the given id, or throws ResourceNotFoundException. */
    public Routine findById(long id) {
        return routines.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("rutina", String.valueOf(id)));
    }

    /** Returns the routines created by the given trainer. */
    public List<Routine> findByTrainer(long trainerId) {
        return routines.findByTrainerId(trainerId);
    }

    /** Validates and stores a new routine. */
    public Routine create(Routine routine) {
        validator.validate(routine);
        ensureReferencesExist(routine);
        // withId(0) fuerza a que el repositorio asigne un id nuevo.
        return routines.save(routine.withId(0));
    }

    /** Validates and stores the changes of an existing routine. */
    public Routine update(Routine routine) {
        findById(routine.getId());
        validator.validate(routine);
        ensureReferencesExist(routine);
        return routines.save(routine);
    }

    // Regla: el entrenador debe existir y estar activo, y todos los ejercicios deben existir.
    // findById lanza ResourceNotFoundException si algo no existe.
    private void ensureReferencesExist(Routine routine) {
        if (!trainers.findById(routine.getTrainerId()).isActive()) {
            throw new BusinessRuleException("El entrenador de la rutina está inactivo");
        }
        for (RoutineDay day : routine.getDays()) {
            for (RoutineExercise line : day.getExercises()) {
                exercises.findById(line.getExerciseId());
            }
        }
    }
}