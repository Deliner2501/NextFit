package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates the NextFit tables in Oracle by running {@code db/schema.sql} with
 * the application user. The script uses IF NOT EXISTS, so it can be run again
 * without damage. Each statement reports its own result, so a failure shows
 * exactly which one failed.
 */
public final class OracleSchemaInstaller {

    private static final String RESOURCE = "/db/schema.sql";
    private static final int SUMMARY_LENGTH = 70;

    private OracleSchemaInstaller() {
    }

    /**
     * Runs the schema from the folder that contains nextfit.properties.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Optional<DatabaseConfig> config = DatabaseConfig.load(Path.of(DatabaseConfig.FILE_NAME));
        if (config.isEmpty()) {
            System.out.println("No existe " + DatabaseConfig.FILE_NAME + ".");
            return;
        }
        int succeeded = 0;
        int failed = 0;
        try (Connection connection = new OracleConnectionProvider(config.get()).open();
             Statement statement = connection.createStatement()) {
            for (String sql : readStatements()) {
                try {
                    statement.execute(sql);
                    succeeded++;
                    System.out.println("OK     " + summary(sql));
                } catch (SQLException e) {
                    failed++;
                    System.out.println("ERROR  " + summary(sql) + " -> " + e.getMessage().trim());
                }
            }
        } catch (PersistenceException | SQLException e) {
            Throwable reason = e.getCause() != null ? e.getCause() : e;
            System.err.println("No se pudo conectar: " + reason.getMessage());
            return;
        }
        System.out.println("Terminado: " + succeeded + " correctas, " + failed + " con error.");
    }

    /**
     * Reads the statements of the schema file: comment lines are dropped and
     * the text is split at each semicolon.
     *
     * @return the statements, without the final semicolon
     */
    static List<String> readStatements() {
        String text = readSchema();
        StringBuilder withoutComments = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        for (String part : withoutComments.toString().split(";")) {
            String sql = part.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    private static String readSchema() {
        try (InputStream in = OracleSchemaInstaller.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new PersistenceException("No se encontró " + RESOURCE, null);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer " + RESOURCE, e);
        }
    }

    private static String summary(String sql) {
        String oneLine = sql.replaceAll("\\s+", " ");
        return oneLine.length() <= SUMMARY_LENGTH ? oneLine : oneLine.substring(0, SUMMARY_LENGTH) + "...";
    }
}
