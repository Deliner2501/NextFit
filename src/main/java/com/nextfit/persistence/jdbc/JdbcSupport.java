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

/** Binds values and maps rows; every database operation is a named PL/SQL call. */
final class JdbcSupport {
    private JdbcSupport() { }

    @FunctionalInterface
    interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rows) throws SQLException;
    }

    static java.sql.CallableStatement prepare(Connection connection, String procedure, int parameters)
            throws SQLException {
        if (!procedure.matches("pkg_nf_[a-z]+\\.[a-z_]+") || parameters < 0) {
            throw new IllegalArgumentException("Procedimiento Oracle no permitido: " + procedure);
        }
        String marks = String.join(", ", java.util.Collections.nCopies(parameters, "?"));
        return connection.prepareCall("{call " + procedure + "(" + marks + ")}");
    }

    static <T> List<T> query(ConnectionSource connections, String procedure, int inputs,
                            StatementBinder binder, RowMapper<T> mapper) {
        try (Connection connection = connections.open();
             var call = prepare(connection, procedure, inputs + 1)) {
            if (binder != null) binder.bind(call);
            call.registerOutParameter(inputs + 1, Types.REF_CURSOR);
            call.execute();
            try (ResultSet rows = (ResultSet) call.getObject(inputs + 1)) {
                List<T> result = new ArrayList<>();
                while (rows.next()) result.add(mapper.map(rows));
                return result;
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo consultar " + procedure, e);
        }
    }

    static long save(ConnectionSource connections, String procedure, long id, int inputs, StatementBinder binder) {
        try (Connection connection = connections.open()) {
            return save(connection, procedure, id, inputs, binder);
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar mediante " + procedure, e);
        }
    }

    static long save(Connection connection, String procedure, long id, int inputs, StatementBinder binder)
            throws SQLException {
        try (var call = prepare(connection, procedure, inputs + 2)) {
            binder.bind(call);
            call.setLong(inputs + 1, id);
            call.registerOutParameter(inputs + 2, Types.NUMERIC);
            call.execute();
            return call.getLong(inputs + 2);
        }
    }

    static void execute(ConnectionSource connections, String procedure, int inputs, StatementBinder binder) {
        try (Connection connection = connections.open(); var call = prepare(connection, procedure, inputs)) {
            if (binder != null) binder.bind(call);
            call.execute();
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo ejecutar " + procedure, e);
        }
    }

    static String emptyIfNull(String text) { return text == null ? "" : text; }

    static void setTimestamp(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        if (value == null) statement.setNull(index, Types.TIMESTAMP);
        else statement.setObject(index, value);
    }
}
