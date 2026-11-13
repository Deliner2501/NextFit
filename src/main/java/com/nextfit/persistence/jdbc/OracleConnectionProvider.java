package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens connections to the Oracle database. It is the only class that knows
 * how to reach Oracle, so the repositories only receive a connection.
 */
public final class OracleConnectionProvider {

    private final DatabaseConfig config;

    /**
     * Creates the provider.
     *
     * @param config the connection settings
     */
    public OracleConnectionProvider(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Opens a new connection. The caller closes it.
     *
     * @return an open connection
     * @throws PersistenceException when Oracle cannot be reached
     */
    public Connection open() {
        try {
            return DriverManager.getConnection(config.url(), config.user(), config.password());
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo conectar con Oracle", e);
        }
    }
}
