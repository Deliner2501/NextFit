package com.nextfit.persistence;

import com.nextfit.model.TrainingSession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CSV implementation of {@link TrainingSessionRepository}. */
public class CsvTrainingSessionRepository implements TrainingSessionRepository {

    private final CsvStore store;

    /** @param file the CSV file where sessions are stored */
    public CsvTrainingSessionRepository(Path file) {
        this.store = new CsvStore(file, TrainingSessionCsvMapper.COLUMNS);
    }

    @Override
    public List<TrainingSession> findAll() {
        List<TrainingSession> sessions = new ArrayList<>();
        for (String[] row : store.readAll()) {
            sessions.add(TrainingSessionCsvMapper.fromRow(row));
        }
        return sessions;
    }

    @Override
    public Optional<TrainingSession> findById(long id) {
        return findAll().stream().filter(s -> s.getId() == id).findFirst();
    }

    @Override
    public List<TrainingSession> findByMemberId(long memberId) {
        return findAll().stream().filter(s -> s.getMemberId() == memberId).toList();
    }

    @Override
    public TrainingSession save(TrainingSession session) {
        List<TrainingSession> sessions = findAll();
        TrainingSession toStore = session;
        if (session.getId() == 0) {
            // Id nuevo: el mayor existente + 1.
            long nextId = sessions.stream().mapToLong(TrainingSession::getId).max().orElse(0) + 1;
            toStore = session.withId(nextId);
            sessions.add(toStore);
        } else {
            // Id existente: se reemplaza la sesión con ese id.
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getId() == session.getId()) {
                    sessions.set(i, session);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (TrainingSession s : sessions) {
            rows.add(TrainingSessionCsvMapper.toRow(s));
        }
        store.writeAll(rows);
        return toStore;
    }
}