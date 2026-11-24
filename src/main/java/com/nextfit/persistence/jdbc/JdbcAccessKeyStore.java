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
        try (var call = JdbcSupport.prepare(connection, "pkg_nf_access.key_initialize", 2)) {
            call.setBytes(1, candidate);
            call.registerOutParameter(2, java.sql.Types.BLOB);
            call.execute();
            return call.getBytes(2);
        }
    }

    static byte[] read(Connection connection) throws SQLException {
        try (var call = JdbcSupport.prepare(connection, "pkg_nf_access.key_read", 1)) {
            call.registerOutParameter(1, java.sql.Types.BLOB);
            call.execute();
            return call.getBytes(1);
        }
    }
}
