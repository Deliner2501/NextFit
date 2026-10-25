package com.nextfit.persistence;

import com.nextfit.model.Subscription;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores subscriptions in a CSV file through {@link CsvStore}.
 */
public class CsvSubscriptionRepository implements SubscriptionRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvSubscriptionRepository(Path file) {
        this.store = new CsvStore(file, SubscriptionCsvMapper.COLUMNS);
    }

    @Override
    public List<Subscription> findAll() {
        List<Subscription> subscriptions = new ArrayList<>();
        for (String[] row : store.readAll()) {
            subscriptions.add(SubscriptionCsvMapper.fromRow(row));
        }
        return subscriptions;
    }

    @Override
    public Optional<Subscription> findById(long id) {
        return findAll().stream().filter(s -> s.getId() == id).findFirst();
    }

    @Override
    public List<Subscription> findByMemberId(long memberId) {
        return findAll().stream().filter(s -> s.getMemberId() == memberId).toList();
    }

    @Override
    public Subscription save(Subscription subscription) {
        List<Subscription> subscriptions = findAll();
        Subscription toStore = subscription;
        if (subscription.getId() == 0) {
            long nextId = subscriptions.stream().mapToLong(Subscription::getId).max().orElse(0) + 1;
            toStore = subscription.withId(nextId);
            subscriptions.add(toStore);
        } else {
            for (int i = 0; i < subscriptions.size(); i++) {
                if (subscriptions.get(i).getId() == subscription.getId()) {
                    subscriptions.set(i, subscription);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Subscription s : subscriptions) {
            rows.add(SubscriptionCsvMapper.toRow(s));
        }
        store.writeAll(rows);
        return toStore;
    }
}
