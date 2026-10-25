package com.nextfit.persistence;

import com.nextfit.model.Trainer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CSV implementation of {@link TrainerRepository}. */
public class CsvTrainerRepository implements TrainerRepository {

    private final CsvStore store;

    /** @param file the CSV file where trainers are stored */
    public CsvTrainerRepository(Path file) {
        this.store = new CsvStore(file, TrainerCsvMapper.COLUMNS);
    }

    @Override
    public List<Trainer> findAll() {
        List<Trainer> trainers = new ArrayList<>();
        for (String[] row : store.readAll()) {
            trainers.add(TrainerCsvMapper.fromRow(row));
        }
        return trainers;
    }

    @Override
    public Optional<Trainer> findById(long id) {
        return findAll().stream().filter(t -> t.getId() == id).findFirst();
    }

    @Override
    public Optional<Trainer> findByDocumentNumber(String documentNumber) {
        return findAll().stream()
                .filter(t -> t.getDocumentNumber().equals(documentNumber))
                .findFirst();
    }

    @Override
    public Trainer save(Trainer trainer) {
        List<Trainer> trainers = findAll();
        Trainer toStore = trainer;
        if (trainer.getId() == 0) {
            // Id nuevo: el mayor existente + 1.
            long nextId = trainers.stream().mapToLong(Trainer::getId).max().orElse(0) + 1;
            toStore = trainer.withId(nextId);
            trainers.add(toStore);
        } else {
            // Id existente: se reemplaza el entrenador con ese id.
            for (int i = 0; i < trainers.size(); i++) {
                if (trainers.get(i).getId() == trainer.getId()) {
                    trainers.set(i, trainer);
                    break;
                }
            }
        }
        List<String[]> rows = new ArrayList<>();
        for (Trainer t : trainers) {
            rows.add(TrainerCsvMapper.toRow(t));
        }
        store.writeAll(rows);
        return toStore;
    }
}