package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.HealthProfile;
import com.nextfit.persistence.HealthProfileRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Stores the health profile of each member in the {@code health_profiles} table.
 * There is one profile per member, so the member id is the key.
 */
public final class JdbcHealthProfileRepository implements HealthProfileRepository {

    private static final String SELECT =
            "SELECT member_id, height_cm, blood_type, medical_conditions, allergies FROM health_profiles";
    private static final String UPDATE =
            "UPDATE health_profiles SET height_cm = ?, blood_type = ?, medical_conditions = ?, allergies = ? "
                    + "WHERE member_id = ?";
    private static final String INSERT =
            "INSERT INTO health_profiles (height_cm, blood_type, medical_conditions, allergies, member_id) "
                    + "VALUES (?, ?, ?, ?, ?)";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcHealthProfileRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public Optional<HealthProfile> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, SELECT + " WHERE member_id = ?",
                statement -> statement.setLong(1, memberId), JdbcHealthProfileRepository::toEntity)
                .stream().findFirst();
    }

    @Override
    public HealthProfile save(HealthProfile value) {
        try (Connection connection = connections.open()) {
            if (write(connection, UPDATE, value) == 0) {
                write(connection, INSERT, value);
            }
            return value;
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar el perfil de salud en la base de datos", e);
        }
    }

    private static int write(Connection connection, String sql, HealthProfile value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, value.getHeightCm());
            statement.setString(2, value.getBloodType());
            statement.setString(3, value.getMedicalConditions());
            statement.setString(4, value.getAllergies());
            statement.setLong(5, value.getMemberId());
            return statement.executeUpdate();
        }
    }

    private static HealthProfile toEntity(ResultSet rows) throws SQLException {
        return new HealthProfile(
                rows.getLong("member_id"),
                rows.getDouble("height_cm"),
                JdbcSupport.emptyIfNull(rows.getString("blood_type")),
                JdbcSupport.emptyIfNull(rows.getString("medical_conditions")),
                JdbcSupport.emptyIfNull(rows.getString("allergies")));
    }
}
