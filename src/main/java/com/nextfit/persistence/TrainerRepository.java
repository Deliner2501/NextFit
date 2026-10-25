package com.nextfit.persistence;

import com.nextfit.model.Trainer;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for trainers. Services depend on this interface only,
 * so the CSV implementation can later be replaced by a database one.
 */
public interface TrainerRepository {

    /** Returns every stored trainer. */
    List<Trainer> findAll();

    /** Returns the trainer with the given id, or empty if it does not exist. */
    Optional<Trainer> findById(long id);

    /** Returns the trainer with the given document number, or empty if none. */
    Optional<Trainer> findByDocumentNumber(String documentNumber);

    /**
     * Stores a trainer. Id 0 means a new trainer (a new id is assigned);
     * any other id replaces the stored trainer. Returns the stored trainer.
     */
    Trainer save(Trainer trainer);
}