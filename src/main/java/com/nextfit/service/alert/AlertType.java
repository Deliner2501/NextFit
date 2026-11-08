package com.nextfit.service.alert;

/**
 * Kinds of alerts the gym staff can receive. The order of the constants is the
 * order of importance: the first ones are shown first.
 */
public enum AlertType {

    /** A membership that ends in a few days and has not been renewed. */
    EXPIRING("Por vencer"),
    /** A member with a valid membership who has not come to the gym for a while. */
    INACTIVE("Inactivo"),
    /** A member whose birthday is today or very soon. */
    BIRTHDAY("Cumpleaños");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    /**
     * Returns the text shown to the staff.
     *
     * @return the label of the type
     */
    public String getLabel() {
        return label;
    }
}
