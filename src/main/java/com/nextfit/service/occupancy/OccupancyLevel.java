package com.nextfit.service.occupancy;

/**
 * How full the gym is, from quiet to full. Each level knows from which percentage of
 * the capacity it starts, so nobody else needs to know those limits.
 */
public enum OccupancyLevel {

    // EXPLICACIÓN: el nivel es un enum porque los niveles son pocos y fijos; "fromPercent"
    // es el porcentaje de aforo desde el cual empieza cada nivel (Experto en información).
    QUIET("Tranquilo", 0),
    MODERATE("Moderado", 40),
    BUSY("Concurrido", 70),
    FULL("Lleno", 90);

    private final String label;
    private final int fromPercent;

    OccupancyLevel(String label, int fromPercent) {
        this.label = label;
        this.fromPercent = fromPercent;
    }

    /**
     * Returns the level for a percentage of the capacity.
     *
     * @param percent the occupancy, from 0 to 100
     * @return the highest level whose limit the percentage reaches
     */
    public static OccupancyLevel of(int percent) {
        OccupancyLevel result = QUIET;
        for (OccupancyLevel level : values()) {
            if (percent >= level.fromPercent) {
                result = level;
            }
        }
        return result;
    }

    /**
     * Returns the text shown to the user.
     *
     * @return the label in Spanish
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the percentage where this level starts.
     *
     * @return the lower limit
     */
    public int getFromPercent() {
        return fromPercent;
    }
}