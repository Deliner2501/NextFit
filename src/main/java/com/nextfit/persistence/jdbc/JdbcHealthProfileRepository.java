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
        return JdbcSupport.query(connections, "pkg_nf_members.health_profiles_by_member", 1,
                statement -> statement.setLong(1, memberId), JdbcHealthProfileRepository::toEntity)
                .stream().findFirst();
    }

    @Override
    public HealthProfile save(HealthProfile value) {
        JdbcSupport.save(connections, "pkg_nf_members.health_profiles_save", value.getMemberId(), 4, statement -> {
            statement.setDouble(1, value.getHeightCm());
            statement.setString(2, value.getBloodType());
            statement.setString(3, value.getMedicalConditions());
            statement.setString(4, value.getAllergies());
        });
        return value;
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
