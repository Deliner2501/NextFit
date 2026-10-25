package com.nextfit.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A training routine created by a trainer: a name, an objective and a list of
 * days, each one with its exercises. The class is immutable.
 */
public class Routine {

    // EXPLICACIÓN: una rutina completa se arma así:
    //   Routine -> varios RoutineDay -> varios RoutineExercise -> un ejercicio del catálogo.
    // trainerId guarda quién la creó. Se guarda el id y no el objeto entrenador,
    // para que la rutina no dependa de la clase Trainer.
    private final long id;
    private final String name;
    private final String objective;
    private final long trainerId;
    private final LocalDate createdAt;
    private final List<RoutineDay> days;

    /**
     * Creates a routine.
     *
     * @param id        the identifier, or 0 when it has not been assigned yet
     * @param name      the routine name
     * @param objective the goal of the routine (for example "gain muscle")
     * @param trainerId the identifier of the trainer who created it
     * @param createdAt the creation date
     * @param days      the days of the routine
     */
    public Routine(long id, String name, String objective, long trainerId,
                   LocalDate createdAt, List<RoutineDay> days) {
        this.id = id;
        this.name = name;
        this.objective = objective;
        this.trainerId = trainerId;
        this.createdAt = createdAt;
        this.days = days == null ? List.of() : List.copyOf(days);
    }

    /**
     * Returns a copy of this routine with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public Routine withId(long newId) {
        return new Routine(newId, name, objective, trainerId, createdAt, days);
    }

    /**
     * Adds up the sets of every day of the routine. The routine builder screen
     * shows it as the total volume.
     *
     * @return the total number of sets
     */
    public int getTotalSets() {
        int total = 0;
        for (RoutineDay day : days) {
            total += day.getTotalSets();
        }
        return total;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getObjective() {
        return objective;
    }

    public long getTrainerId() {
        return trainerId;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public List<RoutineDay> getDays() {
        return days;
    }
}