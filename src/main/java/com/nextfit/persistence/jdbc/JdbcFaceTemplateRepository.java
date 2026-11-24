package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.FaceTemplate;
import com.nextfit.persistence.FaceTemplateRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Stores the face templates of the members in the {@code face_templates} table:
 * one row per template, with the vector as numbers separated by semicolons (the
 * same text the CSV repository uses, so the migration copies it as it is).
 */
public final class JdbcFaceTemplateRepository implements FaceTemplateRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcFaceTemplateRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public java.util.Set<Long> enrolledIds() {
        return java.util.Set.copyOf(JdbcSupport.query(connections, "pkg_nf_access.faces_ids", 0,
                null, rows -> rows.getLong(1)));
    }

    @Override
    public List<FaceTemplate> findAll() {
        return JdbcSupport.query(connections, "pkg_nf_access.faces_all", 0, null,
                rows -> new FaceTemplate(rows.getLong("member_id"), parse(rows.getString("embedding"))));
    }

    @Override
    public boolean has(long memberId) {
        return JdbcSupport.query(connections, "pkg_nf_access.faces_count", 1,
                statement -> statement.setLong(1, memberId), rows -> rows.getLong(1)).get(0) > 0;
    }

    /** Deletes the old rows and inserts the new ones in one transaction: it all happens or nothing does. */
    @Override
    public void replace(long memberId, List<float[]> vectors) {
        try (Connection connection = connections.open()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                deleteRows(connection, memberId);
                insertRows(connection, memberId, vectors);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudieron guardar los rostros del socio en la base de datos", e);
        }
    }

    @Override
    public void delete(long memberId) {
        try (Connection connection = connections.open()) {
            deleteRows(connection, memberId);
        } catch (SQLException e) {
            throw new PersistenceException("No se pudieron borrar los rostros del socio de la base de datos", e);
        }
    }

    private static void deleteRows(Connection connection, long memberId) throws SQLException {
        try (PreparedStatement statement = JdbcSupport.prepare(connection, "pkg_nf_access.faces_delete", 1)) {
            statement.setLong(1, memberId);
            statement.execute();
        }
    }

    private static void insertRows(Connection connection, long memberId, List<float[]> vectors)
            throws SQLException {
        try (PreparedStatement statement = JdbcSupport.prepare(connection, "pkg_nf_access.faces_insert", 2)) {
            for (float[] vector : vectors) {
                statement.setLong(1, memberId);
                statement.setString(2, format(vector));
                statement.execute();
            }
        }
    }

    private static String format(float[] vector) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                text.append(';');
            }
            text.append(vector[i]);
        }
        return text.toString();
    }

    private static float[] parse(String text) {
        String[] parts = text.trim().split(";");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
