package com.nextfit.persistence;

import com.nextfit.model.Routine;

import java.util.List;
import java.util.Optional;

/**
 * Storage of routines, with their days and exercises. Services depend on this
 * interface, so the CSV implementation can be replaced by a database one
 * without changing them.
 */
public interface RoutineRepository {

    /**
     * Returns every stored routine.
     *
     * @return the routines
     */
    List<Routine> findAll();

    /**
     * Looks a routine up by identifier.
     *
     * @param id the identifier
     * @return the routine, or empty when it does not exist
     */
    Optional<Routine> findById(long id);

    /**
     * Returns the routines created by a trainer. The trainer screen uses it to
     * list only his own routines.
     *
     * @param trainerId the identifier of the trainer
     * @return the routines of that trainer
     */
    List<Routine> findByTrainerId(long trainerId);

    /**
     * Inserts the routine when its id is 0 (assigning a new one) or replaces
     * the stored routine with the same id.
     *
     * @param routine the routine to store
     * @return the stored routine, with its identifier
     */
    Routine save(Routine routine);
}