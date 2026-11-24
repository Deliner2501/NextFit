package com.nextfit.persistence.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

/**
 * Test-only bridge for the existing offline repository behavior tests.
 * H2 cannot compile PL/SQL: these fixtures are NOT evidence that Oracle packages compile.
 * OraclePackageIntegrationTest exercises the actual packages, driver and rollback.
 */
final class H2PackageEmulator {
    private static final Properties CONTRACT = new Properties();
    static {
        try (var in = H2PackageEmulator.class.getResourceAsStream("/db/package-contract.properties")) {
            CONTRACT.load(in);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    static Connection wrap(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareCall")) return call(connection, (String) args[0]);
                    try { return method.invoke(connection, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
    }

    private static CallableStatement call(Connection connection, String text) throws SQLException {
        String name = text.substring("{call ".length(), text.indexOf('('));
        String kind = CONTRACT.getProperty(name + ".kind");
        if (kind == null) throw new SQLException("Unknown package test contract: " + name);
        int inputs = Integer.parseInt(CONTRACT.getProperty(name + ".inputs"));
        Map<Integer, Object> values = new HashMap<>();
        Map<Integer, Object> outputs = new HashMap<>();
        List<AutoCloseable> resources = new ArrayList<>();
        return (CallableStatement) Proxy.newProxyInstance(CallableStatement.class.getClassLoader(),
                new Class<?>[]{CallableStatement.class}, (proxy, method, args) -> {
                    String m = method.getName();
                    if (m.equals("registerOutParameter")) return null;
                    if (m.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer index) {
                        values.put(index, m.equals("setNull") ? null : args[1]); return null;
                    }
                    if (m.equals("execute")) {
                        if (kind.equals("query")) {
                            PreparedStatement statement = connection.prepareStatement(CONTRACT.getProperty(name + ".sql"));
                            resources.add(statement); bind(statement, values, inputs);
                            ResultSet rows = statement.executeQuery(); resources.add(rows); outputs.put(inputs + 1, rows);
                        } else if (kind.equals("save")) outputs.put(inputs + 1, save(connection, name, values, inputs));
                        else if (kind.equals("key_read")) outputs.put(1, readKey(connection));
                        else if (kind.equals("key_init")) {
                            try (var s = connection.prepareStatement("INSERT INTO access_settings (id, access_key) VALUES (1, ?)")) {
                                s.setBytes(1, (byte[]) values.get(1)); s.executeUpdate(); outputs.put(2, values.get(1));
                            } catch (SQLException e) {
                                if (!"23505".equals(e.getSQLState())) throw e;
                                outputs.put(2, readKey(connection));
                            }
                        } else if (kind.equals("ping")) outputs.put(1, 1);
                        else if (kind.equals("counter")) throw new SQLFeatureNotSupportedException("Oracle DDL only");
                        else try (var statement = connection.prepareStatement(CONTRACT.getProperty(name + ".sql"))) {
                            bind(statement, values, inputs); statement.executeUpdate();
                        }
                        return false;
                    }
                    if (m.equals("getObject") || m.equals("getBytes")) return outputs.get((Integer) args[0]);
                    if (m.equals("getLong")) return ((Number) outputs.get((Integer) args[0])).longValue();
                    if (m.equals("getInt")) return ((Number) outputs.get((Integer) args[0])).intValue();
                    if (m.equals("close")) { for (var r : resources.reversed()) r.close(); return null; }
                    if (m.equals("toString")) return "H2 test package: " + name;
                    throw new SQLFeatureNotSupportedException("Test bridge: " + m);
                });
    }

    private static void bind(PreparedStatement statement, Map<Integer, Object> values, int inputs) throws SQLException {
        for (int i = 1; i <= inputs; i++) statement.setObject(i, values.get(i));
    }

    private static long save(Connection connection, String name, Map<Integer, Object> values, int inputs) throws SQLException {
        String table = CONTRACT.getProperty(name + ".table");
        String key = CONTRACT.getProperty(name + ".key");
        String[] columns = CONTRACT.getProperty(name + ".columns").split(",");
        long id = ((Number) values.get(inputs)).longValue();
        boolean perMember = !key.equals("id");
        if ((id != 0 || perMember) && !Boolean.parseBoolean(CONTRACT.getProperty(name + ".insertOnly"))) {
            String assignments = String.join(", ", Arrays.stream(columns).map(c -> c + " = ?").toList());
            try (var statement = connection.prepareStatement("UPDATE " + table + " SET " + assignments + " WHERE " + key + " = ?")) {
                bind(statement, values, inputs);
                if (statement.executeUpdate() > 0) return id;
            }
        }
        boolean explicit = id != 0 || perMember;
        String names = String.join(", ", columns) + (explicit ? ", " + key : "");
        String marks = String.join(", ", Collections.nCopies(columns.length + (explicit ? 1 : 0), "?"));
        try (var statement = connection.prepareStatement("INSERT INTO " + table + " (" + names + ") VALUES (" + marks + ")", new String[]{key})) {
            bind(statement, values, columns.length + (explicit ? 1 : 0)); statement.executeUpdate();
            if (explicit) return id;
            try (var keys = statement.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private static byte[] readKey(Connection connection) throws SQLException {
        try (var s = connection.createStatement(); var rows = s.executeQuery("SELECT access_key FROM access_settings WHERE id = 1")) {
            return rows.next() ? rows.getBytes(1) : null;
        }
    }
}
