package com.nextfit.persistence.jdbc;

import com.nextfit.persistence.CsvStorage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Copies the data of the CSV files to the Oracle database. Close NextFit before
 * running it. It can be run again: rows are updated, not repeated. At the end it
 * lines up the counters of the identifiers, so new rows get numbers after the
 * copied ones, and it compares the number of rows on both sides.
 */
public final class CsvToOracleMigration {

    private static final Path DATA_DIR = Path.of("data");
    private static final List<String> TABLES_WITH_COUNTER = List.of("users", "members", "membership_plans",
            "subscriptions", "payments", "attendance", "measurements", "exercises", "trainers", "routines",
            "assignments", "training_sessions", "audit_log", "face_templates");

    private CsvToOracleMigration() {
    }

    /**
     * Runs the migration from the folder that contains nextfit.properties and the data folder.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Optional<DatabaseConfig> config = DatabaseConfig.load(Path.of(DatabaseConfig.FILE_NAME));
        if (config.isEmpty()) {
            System.out.println("No existe " + DatabaseConfig.FILE_NAME + ".");
            return;
        }
        PooledConnectionSource connections = new PooledConnectionSource(new OracleConnectionProvider(config.get()));
        CsvStorage source = new CsvStorage(DATA_DIR);
        JdbcStorage target = new JdbcStorage(connections);

        System.out.println("Copiando de los CSV (" + DATA_DIR + ") a Oracle...");
        DataMigration migration = new DataMigration(source, target);
        boolean allCopied = true;
        for (DataMigration.EntityResult result : migration.run()) {
            System.out.println(result.describe());
            allCopied &= result.isComplete();
        }

        System.out.println();
        resetCounters(connections);

        System.out.println();
        boolean sameCounts = compare(DataMigration.count(source), DataMigration.count(target));
        System.out.println();
        System.out.println(allCopied && sameCounts
                ? "Migracion completa: Oracle tiene todos los datos de los CSV."
                : "Migracion con diferencias: revisa las lineas ERROR y DIFERENTE de arriba.");
    }

    /** After copying rows with their own ids, the next generated id must come after the highest one. */
    private static void resetCounters(ConnectionSource connections) {
        try (Connection connection = connections.open()) {
            for (String table : TABLES_WITH_COUNTER) {
                try (var call = JdbcSupport.prepare(connection, "pkg_nf_system.reset_counter", 1)) {
                    call.setString(1, table);
                    call.execute();
                    System.out.println("OK     contador de " + table + " ajustado");
                } catch (SQLException e) {
                    System.out.println("AVISO  contador de " + table + " no se pudo ajustar: " + e.getMessage().trim());
                }
            }
        } catch (SQLException e) {
            System.out.println("AVISO  no se pudieron ajustar los contadores: " + e.getMessage());
        }
    }

    private static boolean compare(Map<String, Integer> csv, Map<String, Integer> oracle) {
        boolean same = true;
        System.out.println("Comparacion de filas (CSV / Oracle):");
        for (Map.Entry<String, Integer> entry : csv.entrySet()) {
            int inOracle = oracle.getOrDefault(entry.getKey(), 0);
            boolean ok = inOracle >= entry.getValue();
            same &= ok;
            System.out.println((ok ? "OK     " : "DIFERENTE ") + entry.getKey() + ": " + entry.getValue() + " / " + inOracle);
        }
        return same;
    }
}
