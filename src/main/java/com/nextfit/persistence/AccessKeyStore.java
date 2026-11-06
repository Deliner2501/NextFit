package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/**
 * Keeps the secret key of the access codes in a small file. The key is created
 * the first time and read afterwards, so the carnets keep working between runs.
 * Deleting the file creates a new key and invalidates every printed carnet.
 */
public final class AccessKeyStore {

    private static final int KEY_BYTES = 32;

    private AccessKeyStore() {
    }

    /**
     * Reads the key, creating it when the file does not exist or is too short.
     *
     * @param file the path of the key file
     * @return the key
     * @throws PersistenceException when the file cannot be read or written
     */
    public static byte[] loadOrCreate(Path file) {
        try {
            if (Files.isRegularFile(file)) {
                byte[] existing = Files.readAllBytes(file);
                if (existing.length >= KEY_BYTES) {
                    return existing;
                }
            }
            byte[] key = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(key);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, key);
            return key;
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer o crear la clave de acceso", e);
        }
    }
}
