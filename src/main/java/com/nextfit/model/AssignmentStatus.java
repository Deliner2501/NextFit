package com.nextfit.model;

/**
 * Status of a routine assignment to a member.
 */
public enum AssignmentStatus {

    // EXPLICACIÓN: una asignación nace ACTIVE (la rutina está vigente).
    // Pasa a COMPLETED cuando el socio termina el plan, o a CANCELLED si el
    // entrenador la retira antes de tiempo. La regla "un socio solo puede tener
    // una asignación activa a la vez" la aplica el servicio, no este enum.
    ACTIVE("Activa"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada");

    private final String label;

    AssignmentStatus(String label) {
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