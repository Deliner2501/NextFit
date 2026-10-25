package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Trainer;

import java.time.LocalDate;

/** Converts a {@link Trainer} to and from a CSV row. Static helper, never instantiated. */
public final class TrainerCsvMapper {

    /** Column names of the trainers file, in order. */
    public static final String[] COLUMNS = {"id", "fullName", "documentNumber", "phone", "email",
            "birthDate", "specialty", "photoPath", "active"};

    private TrainerCsvMapper() {
    }

    /** Turns a trainer into the text values of one CSV row. */
    public static String[] toRow(Trainer trainer) {
        return new String[]{
                String.valueOf(trainer.getId()),
                trainer.getFullName(),
                trainer.getDocumentNumber(),
                trainer.getPhone(),
                trainer.getEmail(),
                trainer.getBirthDate().toString(),
                trainer.getSpecialty(),
                trainer.getPhotoPath(),
                String.valueOf(trainer.isActive())
        };
    }

    /** Builds a trainer from a CSV row; a damaged row becomes a PersistenceException. */
    public static Trainer fromRow(String[] row) {
        try {
            return new Trainer(
                    Long.parseLong(row[0]),
                    row[1],
                    row[2],
                    row[3],
                    row[4],
                    LocalDate.parse(row[5]),
                    row[6],
                    row[7],
                    Boolean.parseBoolean(row[8]));
        } catch (RuntimeException e) {
            // Cualquier error de formato (número, fecha, columna faltante) se reporta igual.
            throw new PersistenceException("Fila de entrenador inválida en el archivo de entrenadores", e);
        }
    }
}