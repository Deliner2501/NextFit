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

    private static final String SELECT_ALL = "SELECT member_id, embedding FROM face_templates ORDER BY id";
    private static final String COUNT_BY_MEMBER = "SELECT COUNT(*) FROM face_templates WHERE member_id = ?";
    private static final String DELETE_BY_MEMBER = "DELETE FROM face_templates WHERE member_id = ?";
    private static final String INSERT = "INSERT INTO face_templates (member_id, embedding) VALUES (?, ?)";

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
    public List<FaceTemplate> findAll() {
        return JdbcSupport.query(connections, SELECT_ALL, null,
                rows -> new FaceTemplate(rows.getLong("member_id"), parse(rows.getString("embedding"))));
    }

    @Override
    public boolean has(long memberId) {
        return JdbcSupport.query(connections, COUNT_BY_MEMBER,
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
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_MEMBER)) {
            statement.setLong(1, memberId);
            statement.executeUpdate();
        }
    }

    private static void insertRows(Connection connection, long memberId, List<float[]> vectors)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            for (float[] vector : vectors) {
                statement.setLong(1, memberId);
                statement.setString(2, format(vector));
                statement.addBatch();
            }
            statement.executeBatch();
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
