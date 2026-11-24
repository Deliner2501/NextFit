package com.nextfit;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.persistence.CsvStorage;
import com.nextfit.persistence.Storage;
import com.nextfit.persistence.jdbc.ConnectionSource;
import com.nextfit.persistence.jdbc.DatabaseConfig;
import com.nextfit.persistence.jdbc.JdbcStorage;
import com.nextfit.persistence.jdbc.OracleConnectionProvider;
import com.nextfit.persistence.jdbc.PooledConnectionSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Chooses where NextFit keeps its data when it starts. Oracle is used only when
 * nextfit.properties says {@code db.enabled=true} and the database answers; in any
 * other case (no file, not enabled, no internet, missing tables) NextFit keeps
 * working with the CSV files and says why on the console.
 */
public final class StorageSetup {

    private static final int CHECK_SECONDS = 10;

    private StorageSetup() {
    }

    /**
     * Opens the storage that the settings ask for, or the CSV files when Oracle cannot be used.
     *
     * @param dataDir      the folder of the CSV files
     * @param settingsFile the nextfit.properties file
     * @return the storage to give to the services
     */
    public static Storage create(Path dataDir, Path settingsFile) {
        if (!isEnabled(settingsFile)) {
            return new CsvStorage(dataDir);
        }
        try {
            Optional<DatabaseConfig> config = DatabaseConfig.load(settingsFile);
            if (config.isEmpty()) {
                return new CsvStorage(dataDir);
            }
            PooledConnectionSource connections = new PooledConnectionSource(new OracleConnectionProvider(config.get()));
            checkDatabase(connections);
            return new JdbcStorage(connections);
        } catch (PersistenceException | InterruptedException | ExecutionException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("No se pudo usar Oracle (" + reasonOf(e) + "): NextFit usa los archivos CSV.");
            return new CsvStorage(dataDir);
        }
    }

    /** Reads only the db.enabled line; anything other than true means CSV. */
    private static boolean isEnabled(Path settingsFile) {
        if (!Files.exists(settingsFile)) {
            return false;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            return false;
        }
        return Boolean.parseBoolean(properties.getProperty("db.enabled", "false").trim());
    }

    /** Asks the members table how many rows it has, so a missing table or a dead connection shows up now. */
    private static void checkDatabase(ConnectionSource connections)
            throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "oracle-check");
            thread.setDaemon(true);
            return thread;
        });
        try {
            executor.submit(() -> {
                com.nextfit.persistence.jdbc.OraclePackageApi.check(connections);
                return null;
            }).get(CHECK_SECONDS, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static String reasonOf(Exception e) {
        if (e instanceof TimeoutException) {
            return "no respondió en " + CHECK_SECONDS + " segundos";
        }
        Throwable reason = e;
        while (reason.getCause() != null) {
            reason = reason.getCause();
        }
        return reason.getMessage() == null ? e.getClass().getSimpleName() : reason.getMessage().trim();
    }
}
