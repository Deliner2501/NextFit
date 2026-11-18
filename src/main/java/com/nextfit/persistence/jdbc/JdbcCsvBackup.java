package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.persistence.AccessKeyStore;
import com.nextfit.persistence.CsvSnapshot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/** Reads all backup data from the same database transaction. */
final class JdbcCsvBackup {

    private JdbcCsvBackup() {
    }

    static void export(ConnectionSource connections, Path directory) {
        try (Connection connection = connections.open()) {
            int previousIsolation = connection.getTransactionIsolation();
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                Connection shared = withoutClose(connection);
                CsvSnapshot.write(new JdbcStorage(() -> shared), directory);
                byte[] key = JdbcAccessKeyStore.read(connection);
                if (key == null) {
                    throw new PersistenceException("Falta la clave compartida de los carnets", null);
                }
                AccessKeyStore.cache(directory.resolve("access.key"), key);
            } finally {
                connection.rollback();
                connection.setTransactionIsolation(previousIsolation);
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo exportar el respaldo desde Oracle", e);
        }
    }

    // Repositories close their connections after each query. The export owns this one.
    private static Connection withoutClose(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
