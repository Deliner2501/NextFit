package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;

import java.time.LocalDate;
import java.util.Set;

/**
 * Validates health profiles and body measurements before they are stored. It
 * has no access to files or screens, so it can be tested on its own.
 */
public class HealthValidator {

    private static final Set<String> BLOOD_TYPES = Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final double MIN_HEIGHT_CM = 100;
    private static final double MAX_HEIGHT_CM = 250;
    private static final double MIN_WEIGHT_KG = 20;
    private static final double MAX_WEIGHT_KG = 400;
    private static final double MIN_BODY_CM = 20;
    private static final double MAX_BODY_CM = 300;
    private static final int TEXT_MAX = 200;

    /**
     * Validates a health profile.
     *
     * @param profile the profile to check
     * @throws InvalidDataException when any field breaks its rule
     */
    public void validateProfile(HealthProfile profile) {
        validateRange("estatura", profile.getHeightCm(), MIN_HEIGHT_CM, MAX_HEIGHT_CM, "cm");
        if (profile.getBloodType() == null || !BLOOD_TYPES.contains(profile.getBloodType())) {
            throw new InvalidDataException("tipo de sangre", "debe ser A+, A-, B+, B-, AB+, AB-, O+ u O-");
        }
        validateTextLength("condiciones médicas", profile.getMedicalConditions());
        validateTextLength("alergias", profile.getAllergies());
    }

    /**
     * Validates a body measurement.
     *
     * @param measurement the measurement to check
     * @throws InvalidDataException when any field breaks its rule
     */
    public void validateMeasurement(Measurement measurement) {
        if (measurement.getDate() == null) {
            throw new InvalidDataException("fecha", "es obligatoria");
        }
        if (measurement.getDate().isAfter(LocalDate.now())) {
            throw new InvalidDataException("fecha", "no puede ser futura");
        }
        validateRange("peso", measurement.getWeightKg(), MIN_WEIGHT_KG, MAX_WEIGHT_KG, "kg");
        validateRange("cintura", measurement.getWaistCm(), MIN_BODY_CM, MAX_BODY_CM, "cm");
        validateRange("pecho", measurement.getChestCm(), MIN_BODY_CM, MAX_BODY_CM, "cm");
        validateRange("cadera", measurement.getHipsCm(), MIN_BODY_CM, MAX_BODY_CM, "cm");
    }

    private void validateRange(String field, double value, double min, double max, String unit) {
        if (value < min || value > max) {
            throw new InvalidDataException(field,
                    "debe estar entre " + (int) min + " y " + (int) max + " " + unit);
        }
    }

    private void validateTextLength(String field, String text) {
        if (text != null && text.length() > TEXT_MAX) {
            throw new InvalidDataException(field, "no puede superar los " + TEXT_MAX + " caracteres");
        }
    }
}