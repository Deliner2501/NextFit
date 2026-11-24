package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Small program that checks the Oracle connection: it prints the connected
 * user or the reason why it failed. It changes nothing in the database.
 */
public final class OracleConnectionCheck {

    private OracleConnectionCheck() {
    }

    /**
     * Runs the check from the folder that contains nextfit.properties.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Optional<DatabaseConfig> config = DatabaseConfig.load(Path.of(DatabaseConfig.FILE_NAME));
        if (config.isEmpty()) {
            System.out.println("No existe " + DatabaseConfig.FILE_NAME + ": NextFit usaría los archivos CSV.");
            return;
        }
        try (Connection connection = new OracleConnectionProvider(config.get()).open()) {
            System.out.println("Conectado a Oracle como " + connection.getMetaData().getUserName());
        } catch (PersistenceException | SQLException e) {
            Throwable reason = e.getCause() != null ? e.getCause() : e;
            System.err.println("No se pudo conectar: " + reason.getMessage());
        }
    }
}
