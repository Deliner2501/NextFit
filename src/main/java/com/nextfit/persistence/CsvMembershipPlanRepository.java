package com.nextfit.persistence;

import com.nextfit.model.MembershipPlan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores membership plans in a CSV file through {@link CsvStore}.
 */
public class CsvMembershipPlanRepository implements MembershipPlanRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvMembershipPlanRepository(Path file) {
        this.store = new CsvStore(file, MembershipPlanCsvMapper.COLUMNS);
    }

    @Override
    public List<MembershipPlan> findAll() {
        List<MembershipPlan> plans = new ArrayList<>();
        for (String[] row : store.readAll()) {
            plans.add(MembershipPlanCsvMapper.fromRow(row));
        }
        return plans;
    }

    @Override
    public Optional<MembershipPlan> findById(long id) {
        return findAll().stream().filter(p -> p.getId() == id).findFirst();
    }

    @Override
    public Optional<MembershipPlan> findByName(String name) {
        return findAll().stream().filter(p -> p.getName().equalsIgnoreCase(name.trim())).findFirst();
    }

    @Override
    public MembershipPlan save(MembershipPlan plan) {
        List<MembershipPlan> plans = findAll();
        MembershipPlan toStore = plan;
        if (plan.getId() == 0) {
            long nextId = plans.stream().mapToLong(MembershipPlan::getId).max().orElse(0) + 1;
            toStore = plan.withId(nextId);
            plans.add(toStore);
        } else {
            for (int i = 0; i < plans.size(); i++) {
                if (plans.get(i).getId() == plan.getId()) {
                    plans.set(i, plan);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (MembershipPlan p : plans) {
            rows.add(MembershipPlanCsvMapper.toRow(p));
        }
        store.writeAll(rows);
        return toStore;
    }
}
