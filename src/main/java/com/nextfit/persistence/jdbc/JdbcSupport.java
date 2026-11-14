package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers shared by the JDBC repositories, so each repository only says
 * which table it uses and how a row becomes an object. Table and column names
 * always come from constants in the code, never from user input.
 */
final class JdbcSupport {

    private JdbcSupport() {
    }

    /** Puts the values of an object into the parameters of a statement. */
    @FunctionalInterface
    interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Turns the current row of a result into an object. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rows) throws SQLException;
    }

    /**
     * Runs a query and converts every row.
     *
     * @param connections where the connection comes from
     * @param sql         the query
     * @param binder      sets the parameters, or null when there are none
     * @param mapper      converts one row
     * @param <T>         the type of the objects
     * @return the objects, in the order of the query
     */
    static <T> List<T> query(ConnectionSource connections, String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(mapper.map(rows));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo leer de la base de datos", e);
        }
    }

    /**
     * Updates the row with the given id, or inserts it when it does not exist
     * (id 0 lets the database assign the id; another id keeps it, for the migration).
     *
     * @param connections where the connection comes from
     * @param table       the table
     * @param id          the identifier, 0 for a new row
     * @param columns     the data columns, without the id
     * @param binder      sets the values of the columns, from parameter 1
     * @return the identifier of the stored row
     */
    static long save(ConnectionSource connections, String table, long id, String[] columns, StatementBinder binder) {
        try (Connection connection = connections.open()) {
            if (id != 0 && update(connection, table, id, columns, binder) > 0) {
                return id;
            }
            return insert(connection, table, id, columns, binder);
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar en la tabla " + table, e);
        }
    }

    /**
     * Inserts a new row and never updates an existing one.
     *
     * @param connections where the connection comes from
     * @param table       the table
     * @param id          the identifier, 0 to let the database assign it
     * @param columns     the data columns, without the id
     * @param binder      sets the values of the columns, from parameter 1
     * @return the identifier of the new row
     */
    static long insertOnly(ConnectionSource connections, String table, long id, String[] columns,
                           StatementBinder binder) {
        try (Connection connection = connections.open()) {
            return insert(connection, table, id, columns, binder);
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar en la tabla " + table, e);
        }
    }

    /** Oracle stores an empty text as NULL, so NULL is turned back into "". */
    static String emptyIfNull(String text) {
        return text == null ? "" : text;
    }

    /** Sets a date and time that may be missing. */
    static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setObject(index, value);
        }
    }

    private static int update(Connection connection, String table, long id, String[] columns,
                              StatementBinder binder) throws SQLException {
        StringBuilder assignments = new StringBuilder();
        for (String column : columns) {
            if (assignments.length() > 0) {
                assignments.append(", ");
            }
            assignments.append(column).append(" = ?");
        }
        String sql = "UPDATE " + table + " SET " + assignments + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.setLong(columns.length + 1, id);
            return statement.executeUpdate();
        }
    }

    private static long insert(Connection connection, String table, long id, String[] columns,
                               StatementBinder binder) throws SQLException {
        boolean hasId = id != 0;
        String names = String.join(", ", columns) + (hasId ? ", id" : "");
        String marks = "?" + ", ?".repeat(columns.length - 1) + (hasId ? ", ?" : "");
        String sql = "INSERT INTO " + table + " (" + names + ") VALUES (" + marks + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"ID"})) {
            binder.bind(statement);
            if (hasId) {
                statement.setLong(columns.length + 1, id);
            }
            statement.executeUpdate();
            if (hasId) {
                return id;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("La base de datos no devolvió el identificador en " + table, null);
                }
                return keys.getLong(1);
            }
        }
    }
}
