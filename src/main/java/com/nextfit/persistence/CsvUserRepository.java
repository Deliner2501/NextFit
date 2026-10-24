package com.nextfit.persistence;

import com.nextfit.model.User;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores users in a CSV file through {@link CsvStore}.
 */
public class CsvUserRepository implements UserRepository {

    private final CsvStore store;

    /**
     * Opens the repository on the given file, creating it when it does not exist.
     *
     * @param file the CSV file
     */
    public CsvUserRepository(Path file) {
        this.store = new CsvStore(file, UserCsvMapper.COLUMNS);
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        for (String[] row : store.readAll()) {
            users.add(UserCsvMapper.fromRow(row));
        }
        return users;
    }

    @Override
    public Optional<User> findById(long id) {
        return findAll().stream().filter(u -> u.getId() == id).findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return findAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public User save(User user) {
        List<User> users = findAll();
        User toStore = user;
        if (user.getId() == 0) {
            long nextId = users.stream().mapToLong(User::getId).max().orElse(0) + 1;
            toStore = user.withId(nextId);
            users.add(toStore);
        } else {
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId() == user.getId()) {
                    users.set(i, user);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (User u : users) {
            rows.add(UserCsvMapper.toRow(u));
        }
        store.writeAll(rows);
        return toStore;
    }
}