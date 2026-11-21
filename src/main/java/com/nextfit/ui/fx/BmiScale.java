package com.nextfit.ui.fx;

/**
 * The scale drawn by {@link BmiGauge}: four stretches (underweight, normal, overweight,
 * obesity) between 15 and 40, matching the limits of {@code BmiCategory}. It only does
 * the arithmetic, so it can be tested without a screen.
 */
final class BmiScale {

    /** Where each stretch starts; the last value is the end of the scale. */
    private static final double[] LIMITS = {15.0, 18.5, 25.0, 30.0, 40.0};

    private BmiScale() {
    }

    /** @return how many stretches the scale has */
    static int segments() {
        return LIMITS.length - 1;
    }

    /**
     * Tells how much of the bar a stretch takes.
     *
     * @param index the stretch, from 0 (underweight) to 3 (obesity)
     * @return its share of the whole bar, between 0 and 1
     */
    static double share(int index) {
        return (LIMITS[index + 1] - LIMITS[index]) / (LIMITS[LIMITS.length - 1] - LIMITS[0]);
    }

    /**
     * Tells where a BMI falls along the bar.
     *
     * @param bmi the body mass index
     * @return the position, from 0 (left end) to 1 (right end); values outside the scale stay at the ends
     */
    static double position(double bmi) {
        double first = LIMITS[0];
        double last = LIMITS[LIMITS.length - 1];
        double fraction = (bmi - first) / (last - first);
        return Math.max(0.0, Math.min(1.0, fraction));
    }
}
