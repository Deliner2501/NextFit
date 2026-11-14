package com.nextfit.service.churn;

/**
 * How likely it is that a member stops coming. Each level knows the lowest score
 * that reaches it and what the staff should do, so no screen has to decide it.
 */
public enum RiskLevel {

    LOW("Bajo", 0, "Sin acción por ahora"),
    MEDIUM("Medio", 30, "Enviarle un mensaje de ánimo"),
    HIGH("Alto", 60, "Escribirle o llamarlo hoy");

    private final String label;
    private final int fromScore;
    private final String action;

    RiskLevel(String label, int fromScore, String action) {
        this.label = label;
        this.fromScore = fromScore;
        this.action = action;
    }

    /**
     * Returns the level that a score reaches.
     *
     * @param score the risk score, from 0 to 100
     * @return the highest level whose limit the score reaches
     */
    public static RiskLevel of(int score) {
        RiskLevel result = LOW;
        for (RiskLevel level : values()) {
            if (score >= level.fromScore) {
                result = level;
            }
        }
        return result;
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
     * Returns what the staff should do with a member at this level.
     *
     * @return the suggested action, in Spanish
     */
    public String getAction() {
        return action;
    }
}