package com.nextfit.persistence;

import com.nextfit.model.Measurement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Stores measurements in a CSV file through {@link CsvStore}.
 */
public class CsvMeasurementRepository implements MeasurementRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvMeasurementRepository(Path file) {
        this.store = new CsvStore(file, MeasurementCsvMapper.COLUMNS);
    }

    @Override
    public List<Measurement> findByMemberId(long memberId) {
        return findAll().stream()
                .filter(m -> m.getMemberId() == memberId)
                .sorted(Comparator.comparing(Measurement::getDate).thenComparingLong(Measurement::getId))
                .toList();
    }

    @Override
    public Measurement save(Measurement measurement) {
        List<Measurement> measurements = findAll();
        Measurement toStore = measurement;
        if (measurement.getId() == 0) {
            long nextId = measurements.stream().mapToLong(Measurement::getId).max().orElse(0) + 1;
            toStore = measurement.withId(nextId);
            measurements.add(toStore);
        } else {
            for (int i = 0; i < measurements.size(); i++) {
                if (measurements.get(i).getId() == measurement.getId()) {
                    measurements.set(i, measurement);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Measurement m : measurements) {
            rows.add(MeasurementCsvMapper.toRow(m));
        }
        store.writeAll(rows);
        return toStore;
    }

    private List<Measurement> findAll() {
        List<Measurement> measurements = new ArrayList<>();
        for (String[] row : store.readAll()) {
            measurements.add(MeasurementCsvMapper.fromRow(row));
        }
        return measurements;
    }
}