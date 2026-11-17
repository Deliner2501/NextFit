package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An in-memory H2 database in Oracle mode, built with the same {@code schema.sql} that
 * {@link OracleSchemaInstaller} runs on Oracle. Each instance is a new, empty database,
 * so every test starts clean and the JDBC repositories can be tested without internet.
 */
final class H2Database implements ConnectionSource {

    private static final AtomicInteger NUMBER = new AtomicInteger();

    private final String url;
    private final Connection keeper;

    H2Database() {
        url = "jdbc:h2:mem:nextfit" + NUMBER.incrementAndGet() + "_" + System.nanoTime()
                + ";MODE=Oracle;DB_CLOSE_DELAY=-1";
        try {
            keeper = DriverManager.getConnection(url);
            createSchema();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base de datos H2 de prueba", e);
        }
    }

    /** Opens a new connection to this database. */
    @Override
    public Connection open() {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo abrir H2", e);
        }
    }

    /** Deletes the database. */
    void close() {
        try (Statement statement = keeper.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException e) {
            // the database is in memory: it disappears with the test anyway
        }
    }

    /** SYSTIMESTAMP is an Oracle function; H2 spells the same thing CURRENT_TIMESTAMP. */
    private void createSchema() throws SQLException {
        try (Statement statement = keeper.createStatement()) {
            for (String sql : OracleSchemaInstaller.readStatements()) {
                statement.execute(sql.replace("SYSTIMESTAMP", "CURRENT_TIMESTAMP"));
            }
        }
    }
}
