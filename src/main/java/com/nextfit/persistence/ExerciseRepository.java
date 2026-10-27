package com.nextfit.persistence;

import com.nextfit.model.Exercise;

import java.util.List;
import java.util.Optional;

/**
 * Storage of the exercise catalog. Services depend on this interface, so the
 * CSV implementation can be replaced by a database one without changing them.
 */
public interface ExerciseRepository {

    // EXPLICACIÓN: una interfaz solo dice QUÉ se puede hacer (los métodos), no
    // CÓMO. Quien la use no sabe si los datos están en un CSV o en una base de
    // datos. Por eso el servicio nunca dependerá de la clase CSV, solo de esto.

    /**
     * Returns every stored exercise.
     *
     * @return the exercises
     */
    List<Exercise> findAll();

    /**
     * Looks an exercise up by identifier.
     *
     * @param id the identifier
     * @return the exercise, or empty when it does not exist
     */
    Optional<Exercise> findById(long id);

    /**
     * Looks an exercise up by name, ignoring case. It lets the service refuse
     * two exercises with the same name.
     *
     * @param name the exercise name
     * @return the exercise, or empty when it does not exist
     */
    Optional<Exercise> findByName(String name);

    /**
     * Inserts the exercise when its id is 0 (assigning a new one) or replaces
     * the stored exercise with the same id.
     *
     * @param exercise the exercise to store
     * @return the stored exercise, with its identifier
     */
    Exercise save(Exercise exercise);
}