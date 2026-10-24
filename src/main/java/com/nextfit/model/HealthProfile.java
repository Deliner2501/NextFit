package com.nextfit.model;

/**
 * The health information of one member that does not change from one visit to
 * the next. The weight is not here because it changes over time: it is kept
 * in {@link Measurement}.
 */
public class HealthProfile {

    private final long memberId;
    private final double heightCm;
    private final String bloodType;
    private final String medicalConditions;
    private final String allergies;

    /**
     * Creates a health profile.
     *
     * @param memberId          the identifier of the member it belongs to
     * @param heightCm          the height in centimeters
     * @param bloodType         the blood type, e.g. "O+"
     * @param medicalConditions the conditions the trainers must know about, or an empty text
     * @param allergies         the known allergies, or an empty text
     */
    public HealthProfile(long memberId, double heightCm, String bloodType,
                         String medicalConditions, String allergies) {
        this.memberId = memberId;
        this.heightCm = heightCm;
        this.bloodType = bloodType;
        this.medicalConditions = medicalConditions;
        this.allergies = allergies;
    }

    public long getMemberId() {
        return memberId;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public String getBloodType() {
        return bloodType;
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public String getAllergies() {
        return allergies;
    }
}