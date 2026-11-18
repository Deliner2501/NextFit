package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.persistence.AccessKeyStore;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Shares the carnet signing key across installations using the same database. */
final class JdbcAccessKeyStore {

    private final ConnectionSource connections;

    JdbcAccessKeyStore(ConnectionSource connections) {
        this.connections = connections;
    }

    byte[] loadOrInitialize(Path localFile) {
        try (Connection connection = connections.open()) {
            byte[] key = read(connection);
            if (key == null) {
                byte[] candidate = AccessKeyStore.loadOrCreate(localFile);
                key = insertOrReadWinner(connection, candidate);
            }
            AccessKeyStore.cache(localFile, key);
            return key;
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo leer o guardar la clave de acceso en Oracle. "
                    + "Comprueba que se instaló el esquema de NextFit.", e);
        }
    }

    private static byte[] insertOrReadWinner(Connection connection, byte[] candidate) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO access_settings (id, access_key) VALUES (1, ?)")) {
            insert.setBytes(1, candidate);
            insert.executeUpdate();
            return candidate;
        } catch (SQLException e) {
            if (e.getErrorCode() != 1 && !"23505".equals(e.getSQLState())) {
                throw e;
            }
            // A concurrent first start already inserted its key.
            byte[] winner = read(connection);
            if (winner == null) {
                throw e;
            }
            return winner;
        }
    }

    static byte[] read(Connection connection) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                     "SELECT access_key FROM access_settings WHERE id = 1");
             ResultSet rows = select.executeQuery()) {
            return rows.next() ? rows.getBytes(1) : null;
        }
    }
}
