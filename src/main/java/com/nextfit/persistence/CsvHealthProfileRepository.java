package com.nextfit.persistence;

import com.nextfit.model.HealthProfile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores health profiles in a CSV file through {@link CsvStore}.
 */
public class CsvHealthProfileRepository implements HealthProfileRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvHealthProfileRepository(Path file) {
        this.store = new CsvStore(file, HealthProfileCsvMapper.COLUMNS);
    }

    @Override
    public Optional<HealthProfile> findByMemberId(long memberId) {
        return findAll().stream().filter(p -> p.getMemberId() == memberId).findFirst();
    }

    @Override
    public HealthProfile save(HealthProfile profile) {
        List<HealthProfile> profiles = findAll();
        boolean replaced = false;
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getMemberId() == profile.getMemberId()) {
                profiles.set(i, profile);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            profiles.add(profile);
        }
        List<String[]> rows = new ArrayList<>();
        for (HealthProfile p : profiles) {
            rows.add(HealthProfileCsvMapper.toRow(p));
        }
        store.writeAll(rows);
        return profile;
    }

    private List<HealthProfile> findAll() {
        List<HealthProfile> profiles = new ArrayList<>();
        for (String[] row : store.readAll()) {
            profiles.add(HealthProfileCsvMapper.fromRow(row));
        }
        return profiles;
    }
}