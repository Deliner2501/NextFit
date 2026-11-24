package com.nextfit.persistence.jdbc;

import com.nextfit.model.Measurement;
import com.nextfit.persistence.MeasurementRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Stores body measurements in the {@code measurements} table.
 */
public final class JdbcMeasurementRepository implements MeasurementRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcMeasurementRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Measurement> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, "pkg_nf_members.measurements_by_member", 1,
                statement -> statement.setLong(1, memberId), JdbcMeasurementRepository::toEntity);
    }

    @Override
    public Measurement save(Measurement value) {
        long id = JdbcSupport.save(connections, "pkg_nf_members.measurements_save", value.getId(), 6, statement -> {
            statement.setLong(1, value.getMemberId());
            statement.setObject(2, value.getDate());
            statement.setDouble(3, value.getWeightKg());
            statement.setDouble(4, value.getWaistCm());
            statement.setDouble(5, value.getChestCm());
            statement.setDouble(6, value.getHipsCm());
        });
        return new Measurement(id, value.getMemberId(), value.getDate(), value.getWeightKg(), value.getWaistCm(), value.getChestCm(), value.getHipsCm());
    }

    private static Measurement toEntity(ResultSet rows) throws SQLException {
        return new Measurement(
                rows.getLong("id"),
                rows.getLong("member_id"),
                rows.getObject("measured_on", LocalDate.class),
                rows.getDouble("weight_kg"),
                rows.getDouble("waist_cm"),
                rows.getDouble("chest_cm"),
                rows.getDouble("hips_cm"));
    }
}
