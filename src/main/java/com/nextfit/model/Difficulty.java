package com.nextfit.model;

/**
 * Difficulty level of an exercise.
 */
public enum Difficulty {

    // EXPLICACIÓN: sirve para que el entrenador filtre el catálogo según la
    // experiencia del socio. Es un enum por la misma razón que MuscleGroup:
    // solo se permiten estos tres valores.
    BEGINNER("Principiante"),
    INTERMEDIATE("Intermedio"),
    ADVANCED("Avanzado");

    private final String label;

    Difficulty(String label) {
        this.label = label;
    }

    /**
     * Returns the Spanish name shown in the user interface.
     *
     * @return the display name
     */
    public String getLabel() {
        return label;
    }
}