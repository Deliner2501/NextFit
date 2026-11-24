package com.nextfit.persistence.jdbc;

import com.nextfit.model.Member;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Checks {@link JdbcMemberRepository} against the real database: it saves a
 * test member, reads it back, changes it and deletes it, so nothing is left.
 */
public final class JdbcMemberRepositoryCheck {

    private JdbcMemberRepositoryCheck() {
    }

    /**
     * Runs the check from the folder that contains nextfit.properties.
     *
     * @param args not used
     * @throws SQLException when the cleanup fails
     */
    public static void main(String[] args) throws SQLException {
        Optional<DatabaseConfig> config = DatabaseConfig.load(Path.of(DatabaseConfig.FILE_NAME));
        if (config.isEmpty()) {
            System.out.println("No existe " + DatabaseConfig.FILE_NAME + ".");
            return;
        }
        OracleConnectionProvider provider = new OracleConnectionProvider(config.get());
        JdbcMemberRepository repository = new JdbcMemberRepository(provider);
        String document = "TEST" + System.currentTimeMillis();
        long id = 0;
        try {
            Member saved = repository.save(new Member(0, "Prueba Oracle", document, "3000000000",
                    "prueba@nextfit.test", LocalDate.of(2000, 1, 1), LocalDate.now(), "", "", true));
            id = saved.getId();
            System.out.println("1. Guardado con id " + id);
            System.out.println("2. findById: " + repository.findById(id).map(Member::getFullName).orElse("NO ENCONTRADO"));
            System.out.println("3. findByDocumentNumber: "
                    + repository.findByDocumentNumber(document).map(Member::getFullName).orElse("NO ENCONTRADO"));
            repository.save(new Member(id, "Prueba Oracle Editada", document, "3000000000",
                    "prueba@nextfit.test", LocalDate.of(2000, 1, 1), LocalDate.now(), "", "", false));
            Member edited = repository.findById(id).orElseThrow();
            System.out.println("4. Editado: " + edited.getFullName() + ", activo=" + edited.isActive());
            System.out.println("5. Total de socios en la base: " + repository.findAll().size());
        } finally {
            if (id != 0) {
                try (Connection connection = provider.open();
                     PreparedStatement delete = connection.prepareStatement("DELETE FROM members WHERE id = ?")) {
                    delete.setLong(1, id);
                    delete.executeUpdate();
                    System.out.println("6. Socio de prueba borrado");
                }
            }
        }
    }
}
