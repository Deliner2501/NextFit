package com.nextfit.model;

/**
 * Goal a member wants to reach with a routine. Each goal knows how many sets,
 * repetitions and rest seconds its exercises use.
 */
public enum TrainingGoal {

    // EXPLICACIÓN: es un enum porque solo existen estos tres objetivos. Además
    // de la etiqueta en español, cada objetivo guarda sus propios números de
    // series, repeticiones y descanso. Así quien arma la rutina le pregunta al
    // objetivo "¿cuánto?" en vez de tener un if gigante (GRASP Information
    // Expert: el que tiene los datos es el que responde).
    MUSCLE_GAIN("Ganar músculo", 4, 8, 90, false),
    WEIGHT_LOSS("Bajar de peso", 3, 15, 45, true),
    TONING("Tonificar", 3, 12, 60, false);

    private final String label;
    private final int sets;
    private final int repetitions;
    private final int restSeconds;
    private final boolean cardioEveryDay;

    TrainingGoal(String label, int sets, int repetitions, int restSeconds, boolean cardioEveryDay) {
        this.label = label;
        this.sets = sets;
        this.repetitions = repetitions;
        this.restSeconds = restSeconds;
        this.cardioEveryDay = cardioEveryDay;
    }

    /**
     * Returns the Spanish name shown in the user interface.
     *
     * @return the display name
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns how many sets each exercise has for this goal.
     *
     * @return the number of sets
     */
    public int getSets() {
        return sets;
    }

    /**
     * Returns how many repetitions each set has for this goal.
     *
     * @return the number of repetitions
     */
    public int getRepetitions() {
        return repetitions;
    }

    /**
     * Returns the default rest between sets for this goal.
     *
     * @return the rest in seconds
     */
    public int getRestSeconds() {
        return restSeconds;
    }

    /**
     * Tells whether every training day of this goal must include a cardio exercise.
     *
     * @return true when cardio is part of every day
     */
    public boolean requiresCardioEveryDay() {
        return cardioEveryDay;
    }
}