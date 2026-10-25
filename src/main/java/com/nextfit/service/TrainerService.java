package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Trainer;
import com.nextfit.persistence.TrainerRepository;
import com.nextfit.validation.TrainerValidator;

import java.util.List;

/**
 * Business rules for trainers. It depends on the repository interface and the
 * validator, never on CSV, so storage can change without touching this class.
 */
public class TrainerService {

    private final TrainerRepository trainers;
    private final TrainerValidator validator;

    public TrainerService(TrainerRepository trainers, TrainerValidator validator) {
        this.trainers = trainers;
        this.validator = validator;
    }

    /** Returns all trainers. */
    public List<Trainer> findAll() {
        return trainers.findAll();
    }

    /** Returns the trainer with the given id, or throws ResourceNotFoundException. */
    public Trainer findById(long id) {
        return trainers.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("entrenador", String.valueOf(id)));
    }

    /** Validates and stores a new trainer; the document number must be unused. */
    public Trainer register(Trainer trainer) {
        validator.validate(trainer);
        ensureDocumentIsFree(trainer);
        // withId(0) fuerza a que el repositorio asigne un id nuevo.
        return trainers.save(trainer.withId(0));
    }

    /** Validates and stores the changes of an existing trainer. */
    public Trainer update(Trainer trainer) {
        findById(trainer.getId());
        validator.validate(trainer);
        ensureDocumentIsFree(trainer);
        return trainers.save(trainer);
    }

    /** Marks a trainer as inactive (trainers are never deleted). */
    public Trainer deactivate(long id) {
        return trainers.save(findById(id).withActive(false));
    }

    /** Marks a trainer as active again. */
    public Trainer reactivate(long id) {
        return trainers.save(findById(id).withActive(true));
    }

    // Regla: dos entrenadores distintos no pueden compartir documento.
    private void ensureDocumentIsFree(Trainer trainer) {
        boolean usedByAnother = trainers.findByDocumentNumber(trainer.getDocumentNumber())
                .filter(other -> other.getId() != trainer.getId())
                .isPresent();
        if (usedByAnother) {
            throw new BusinessRuleException(
                    "Ya existe un entrenador registrado con el documento " + trainer.getDocumentNumber());
        }
    }
}