package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Measurement;

import java.time.LocalDate;

/**
 * Converts measurements to and from CSV rows. It only knows the column layout.
 * Dates are written in ISO format (for example 2026-10-24).
 */
public final class MeasurementCsvMapper {

    /** Header of the measurements file. */
    public static final String[] COLUMNS = {"id", "memberId", "date", "weightKg", "waistCm", "chestCm", "hipsCm"};

    private MeasurementCsvMapper() {
    }

    /**
     * Converts a measurement to a CSV row.
     *
     * @param measurement the measurement
     * @return the row
     */
    public static String[] toRow(Measurement measurement) {
        return new String[]{
                String.valueOf(measurement.getId()),
                String.valueOf(measurement.getMemberId()),
                measurement.getDate().toString(),
                String.valueOf(measurement.getWeightKg()),
                String.valueOf(measurement.getWaistCm()),
                String.valueOf(measurement.getChestCm()),
                String.valueOf(measurement.getHipsCm())
        };
    }

    /**
     * Converts a CSV row to a measurement.
     *
     * @param row the row
     * @return the measurement
     * @throws PersistenceException when the row is malformed
     */
    public static Measurement fromRow(String[] row) {
        try {
            return new Measurement(
                    Long.parseLong(row[0]),
                    Long.parseLong(row[1]),
                    LocalDate.parse(row[2]),
                    Double.parseDouble(row[3]),
                    Double.parseDouble(row[4]),
                    Double.parseDouble(row[5]),
                    Double.parseDouble(row[6]));
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de medición inválida en el archivo de mediciones", e);
        }
    }
}