package com.nextfit.persistence.jdbc;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.persistence.AttendanceRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stores gym entries and exits in the {@code attendance} table.
 */
public final class JdbcAttendanceRepository implements AttendanceRepository {

    private static final String TABLE = "attendance";
    private static final String[] DATA = {"member_id", "check_in", "check_out"};
    private static final String SELECT = "SELECT id, member_id, check_in, check_out FROM attendance";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcAttendanceRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<AttendanceRecord> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcAttendanceRepository::toEntity);
    }

    @Override
    public List<AttendanceRecord> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, SELECT + " WHERE member_id = ? ORDER BY id",
                statement -> statement.setLong(1, memberId), JdbcAttendanceRepository::toEntity);
    }

    @Override
    public Optional<AttendanceRecord> findOpenByMemberId(long memberId) {
        return JdbcSupport.query(connections,
                SELECT + " WHERE member_id = ? AND check_out IS NULL ORDER BY check_in DESC FETCH FIRST 1 ROWS ONLY",
                statement -> statement.setLong(1, memberId), JdbcAttendanceRepository::toEntity).stream().findFirst();
    }

    @Override
    public AttendanceRecord save(AttendanceRecord value) {
        long id = JdbcSupport.save(connections, TABLE, value.getId(), DATA, statement -> {
            statement.setLong(1, value.getMemberId());
            JdbcSupport.setTimestamp(statement, 2, value.getCheckIn());
            JdbcSupport.setTimestamp(statement, 3, value.getCheckOut().orElse(null));
        });
        return new AttendanceRecord(id, value.getMemberId(), value.getCheckIn(), value.getCheckOut().orElse(null));
    }

    private static AttendanceRecord toEntity(ResultSet rows) throws SQLException {
        return new AttendanceRecord(
                rows.getLong("id"),
                rows.getLong("member_id"),
                rows.getObject("check_in", LocalDateTime.class),
                rows.getObject("check_out", LocalDateTime.class));
    }
}
