package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

/** Installs the additive API separately from tables and checks its version at startup. */
public final class OraclePackageApi {
    private OraclePackageApi() { }

    public static void check(ConnectionSource connections) {
        try (Connection connection = connections.open();
             var call = JdbcSupport.prepare(connection, "pkg_nf_system.ping", 1)) {
            call.registerOutParameter(1, Types.NUMERIC);
            call.execute();
            if (call.getInt(1) != 1) throw new SQLException("Version de API Oracle incompatible");
        } catch (SQLException e) {
            throw new PersistenceException("Instala db/package-api.sql en el esquema de NextFit", e);
        }
    }

    /** SQL Developer slash-delimited PL/SQL units, not split at internal semicolons. */
    static List<String> readUnits() {
        try (var in = OraclePackageApi.class.getResourceAsStream("/db/package-api.sql")) {
            if (in == null) throw new IllegalStateException("No existe db/package-api.sql");
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            script = script.replaceAll("(?m)^--.*$", "")
                    .replaceAll("(?m)^(SET |WHENEVER ).*$", "");
            return Arrays.stream(script.split("(?m)^/\\s*$"))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (java.io.IOException e) {
            throw new PersistenceException("No se pudo leer la API Oracle", e);
        }
    }

    /** Uses an optional settings path; never prints passwords or connection settings. */
    public static void main(String[] args) throws SQLException {
        Path file = Path.of(args.length == 0 ? DatabaseConfig.FILE_NAME : args[0]);
        var config = DatabaseConfig.load(file).orElseThrow(() -> new IllegalArgumentException("Falta configuracion"));
        var provider = new OracleConnectionProvider(config);
        try (Connection connection = provider.open(); var statement = connection.createStatement()) {
            System.out.println("Instalando API como " + connection.getMetaData().getUserName());
            for (String unit : readUnits()) {
                statement.execute(unit);
                System.out.println("OK " + unit.lines().findFirst().orElse("unidad"));
            }
        }
        check(provider);
        System.out.println("API Oracle v1 instalada y comprobada.");
    }
}
