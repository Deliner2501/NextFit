package com.nextfit.model;

/**
 * Access level of a system user. It decides which modules the user can open.
 */
public enum Role {

    ADMIN("Administrador"),
    RECEPTION("Recepción"),
    TRAINER("Entrenador");

    private final String label;

    Role(String label) {
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