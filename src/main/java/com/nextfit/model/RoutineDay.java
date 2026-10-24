package com.nextfit.model;

import java.util.List;

/**
 * One day of a routine, for example "Monday - Chest", with its ordered list of
 * exercises. The class is immutable.
 */
public class RoutineDay {

    private final String name;
    private final List<RoutineExercise> exercises;

    /**
     * Creates a routine day.
     *
     * @param name      the day name shown to the user
     * @param exercises the exercises of the day, in the order they are done
     */
    public RoutineDay(String name, List<RoutineExercise> exercises) {
        this.name = name;
        // EXPLICACIÓN: List.copyOf hace una copia que no se puede modificar.
        // Así nadie de afuera puede agregar o quitar ejercicios de este día
        // sin pasar por una nueva copia. El orden de la lista es el orden en que
        // se hacen los ejercicios, por eso no hace falta un campo "orden" aparte.
        this.exercises = exercises == null ? List.of() : List.copyOf(exercises);
    }

    /**
     * Adds up the sets of every exercise of the day.
     *
     * @return the total number of sets
     */
    public int getTotalSets() {
        // EXPLICACIÓN: el día es quien conoce sus ejercicios, así que es el
        // indicado para sumar sus series (patrón GRASP Information Expert).
        int total = 0;
        for (RoutineExercise exercise : exercises) {
            total += exercise.getSets();
        }
        return total;
    }

    public String getName() {
        return name;
    }

    public List<RoutineExercise> getExercises() {
        return exercises;
    }
}