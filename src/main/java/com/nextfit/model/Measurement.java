package com.nextfit.model;

import java.time.LocalDate;

/**
 * The body measurements of one member taken on one date. Keeping several of
 * them for the same member lets the system draw how the body evolves.
 */
public class Measurement {

    private final long id;
    private final long memberId;
    private final LocalDate date;
    private final double weightKg;
    private final double waistCm;
    private final double chestCm;
    private final double hipsCm;

    /**
     * Creates a measurement.
     *
     * @param id       the identifier, or 0 when it has not been assigned yet
     * @param memberId the identifier of the member who was measured
     * @param date     the date the measurement was taken
     * @param weightKg the weight in kilograms
     * @param waistCm  the waist circumference in centimeters
     * @param chestCm  the chest circumference in centimeters
     * @param hipsCm   the hip circumference in centimeters
     */
    public Measurement(long id, long memberId, LocalDate date, double weightKg,
                       double waistCm, double chestCm, double hipsCm) {
        this.id = id;
        this.memberId = memberId;
        this.date = date;
        this.weightKg = weightKg;
        this.waistCm = waistCm;
        this.chestCm = chestCm;
        this.hipsCm = hipsCm;
    }

    /**
     * Returns a copy of this measurement with the given identifier.
     *
     * @param newId the identifier to assign
     * @return the copy
     */
    public Measurement withId(long newId) {
        return new Measurement(newId, memberId, date, weightKg, waistCm, chestCm, hipsCm);
    }

    public long getId() {
        return id;
    }

    public long getMemberId() {
        return memberId;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public double getWaistCm() {
        return waistCm;
    }

    public double getChestCm() {
        return chestCm;
    }

    public double getHipsCm() {
        return hipsCm;
    }
}