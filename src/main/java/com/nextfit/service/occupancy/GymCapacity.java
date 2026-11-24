package com.nextfit.service.occupancy;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.PersistenceException;
import com.nextfit.service.Session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Keeps the admission limit for this NextFit installation. */
public final class GymCapacity {
    public static final int DEFAULT_LIMIT = 60;

    private final Path path;
    private volatile int limit;

    /** @param path the local capacity settings file */
    public GymCapacity(Path path) {
        this.path = path;
        try {
            limit = Files.exists(path) ? Integer.parseInt(Files.readString(path).strip()) : DEFAULT_LIMIT;
            validate(limit);
        } catch (IOException | NumberFormatException e) {
            throw new PersistenceException("No se pudo leer el aforo del gimnasio", e);
        }
    }

    /** @return the current maximum number of visitors */
    public int limit() {
        return limit;
    }

    /**
     * Changes the capacity after checking the current operator's role.
     *
     * @param value the new maximum
     * @param session the current operator
     */
    public synchronized void update(int value, Session session) {
        session.requireAdmin();
        validate(value);
        Path temporary = null;
        try {
            Path directory = path.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, "capacity-", ".tmp");
            Files.writeString(temporary, Integer.toString(value));
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            limit = value;
        } catch (IOException failure) {
            throw new PersistenceException("No se pudo guardar el aforo", failure);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The previous capacity remains usable.
                }
            }
        }
    }

    private static void validate(int value) {
        if (value < 1) {
            throw new InvalidDataException("aforo", "debe ser un entero mayor que cero");
        }
    }
}
