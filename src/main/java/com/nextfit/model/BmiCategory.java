package com.nextfit.model;

/**
 * The weight category that corresponds to a body mass index, following the
 * limits of the World Health Organization.
 */
public enum BmiCategory {

    UNDERWEIGHT("Bajo peso"),
    NORMAL("Peso normal"),
    OVERWEIGHT("Sobrepeso"),
    OBESITY("Obesidad");

    private static final double UNDERWEIGHT_LIMIT = 18.5;
    private static final double NORMAL_LIMIT = 25.0;
    private static final double OVERWEIGHT_LIMIT = 30.0;

    private final String label;

    BmiCategory(String label) {
        this.label = label;
    }

    /**
     * Returns the category of a body mass index.
     *
     * @param bmi the body mass index
     * @return the category it belongs to
     */
    public static BmiCategory fromBmi(double bmi) {
        if (bmi < UNDERWEIGHT_LIMIT) {
            return UNDERWEIGHT;
        }
        if (bmi < NORMAL_LIMIT) {
            return NORMAL;
        }
        if (bmi < OVERWEIGHT_LIMIT) {
            return OVERWEIGHT;
        }
        return OBESITY;
    }

    /**
     * Returns the Spanish name shown in the interface.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }
}