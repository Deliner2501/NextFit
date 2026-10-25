package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.HealthProfile;

/**
 * Converts health profiles to and from CSV rows. It only knows the column layout.
 */
public final class HealthProfileCsvMapper {

    /** Header of the health profiles file. */
    public static final String[] COLUMNS = {"memberId", "heightCm", "bloodType", "medicalConditions", "allergies"};

    private HealthProfileCsvMapper() {
    }

    /**
     * Converts a health profile to a CSV row.
     *
     * @param profile the profile
     * @return the row
     */
    public static String[] toRow(HealthProfile profile) {
        return new String[]{
                String.valueOf(profile.getMemberId()),
                String.valueOf(profile.getHeightCm()),
                profile.getBloodType(),
                profile.getMedicalConditions(),
                profile.getAllergies()
        };
    }

    /**
     * Converts a CSV row to a health profile.
     *
     * @param row the row
     * @return the profile
     * @throws PersistenceException when the row is malformed
     */
    public static HealthProfile fromRow(String[] row) {
        try {
            return new HealthProfile(
                    Long.parseLong(row[0]),
                    Double.parseDouble(row[1]),
                    row[2],
                    row[3],
                    row[4]);
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de perfil de salud inválida en el archivo de salud", e);
        }
    }
}