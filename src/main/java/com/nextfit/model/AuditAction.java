package com.nextfit.model;

/**
 * Kind of event that is written in the audit log.
 */
public enum AuditAction {

    LOGIN("Inicio de sesión"),
    LOGIN_FAILED("Inicio de sesión fallido"),
    LOCKOUT("Usuario bloqueado"),
    LOGOUT("Cierre de sesión"),
    SESSION_EXPIRED("Sesión cerrada por inactividad"),
    USER_CREATED("Usuario creado"),
    USER_UPDATED("Usuario modificado"),
    PASSWORD_RESET("Contraseña restablecida"),
    USER_DEACTIVATED("Usuario desactivado"),
    USER_REACTIVATED("Usuario reactivado"),
    BACKUP_CREATED("Respaldo creado"),
    BACKUP_RESTORED("Respaldo restaurado");

    private final String label;

    AuditAction(String label) {
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