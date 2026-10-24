package com.nextfit.model;

/**
 * Muscle group worked by an exercise.
 */
public enum MuscleGroup {

    // EXPLICACIÓN: un enum es una lista cerrada de valores permitidos.
    // Así un ejercicio solo puede tener uno de estos grupos y nadie escribe
    // "pecho", "Pecho" o "Pechoo" por error. El texto en mayúsculas es el valor
    // interno (el que se guarda en el CSV); el texto entre paréntesis es la
    // etiqueta en español que ve el usuario en pantalla.
    CHEST("Pecho"),
    BACK("Espalda"),
    SHOULDERS("Hombros"),
    ARMS("Brazos"),
    CORE("Abdomen"),
    LEGS("Piernas"),
    GLUTES("Glúteos"),
    FULL_BODY("Cuerpo completo"),
    CARDIO("Cardio");

    private final String label;

    MuscleGroup(String label) {
        this.label = label;
    }

    /**
     * Returns the Spanish name shown in the user interface.
     *
     * @return the display name
     */
    public String getLabel() {
        // EXPLICACIÓN: las pantallas llaman a este método para mostrar
        // "Pecho" en vez de "CHEST".
        return label;
    }
}