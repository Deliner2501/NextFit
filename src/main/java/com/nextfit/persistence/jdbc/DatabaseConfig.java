package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Connection settings of the Oracle database, read from a local file that is
 * never committed. When the file does not exist NextFit keeps using the CSV
 * files, so a missing file is a normal situation and not an error.
 *
 * @param url      the JDBC URL (service name and wallet folder)
 * @param user     the application user
 * @param password the password of the application user
 */
public record DatabaseConfig(String url, String user, String password) {

    /** Name of the local settings file, placed in the folder where NextFit runs. */
    public static final String FILE_NAME = "nextfit.properties";

    /**
     * Reads the settings from a file.
     *
     * @param file the settings file
     * @return the settings, or empty when the file does not exist
     * @throws PersistenceException when the file cannot be read or a setting is missing
     */
    public static Optional<DatabaseConfig> load(Path file) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer el archivo " + FILE_NAME, e);
        }
        return Optional.of(new DatabaseConfig(
                required(properties, "db.url"),
                required(properties, "db.user"),
                required(properties, "db.password")));
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new PersistenceException("Falta la línea " + key + " en " + FILE_NAME, (Throwable) null);
        }
        return value.trim();
    }

    /** Hides the password so it never appears in logs. */
    @Override
    public String toString() {
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=***]";
    }
}
