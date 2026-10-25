package com.nextfit.model;

/**
 * Situation of a subscription on a given day.
 */
public enum SubscriptionStatus {

    UPCOMING("Por iniciar"),
    ACTIVE("Activa"),
    EXPIRED("Vencida"),
    CANCELLED("Cancelada");

    private final String label;

    SubscriptionStatus(String label) {
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