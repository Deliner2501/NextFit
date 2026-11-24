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
        return JdbcSupport.query(connections, "pkg_nf_access.attendance_all", 0, null, JdbcAttendanceRepository::toEntity);
    }

    @Override
    public List<AttendanceRecord> findOpen() {
        return JdbcSupport.query(connections, "pkg_nf_access.attendance_open", 0,
                null, JdbcAttendanceRepository::toEntity);
    }

    @Override
    public long countOpen() {
        return JdbcSupport.query(connections, "pkg_nf_access.attendance_count_open", 0,
                null, rows -> rows.getLong(1)).getFirst();
    }

    @Override
    public List<AttendanceRecord> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, "pkg_nf_access.attendance_by_member", 1,
                statement -> statement.setLong(1, memberId), JdbcAttendanceRepository::toEntity);
    }

    @Override
    public Optional<AttendanceRecord> findOpenByMemberId(long memberId) {
        return JdbcSupport.query(connections,
                "pkg_nf_access.attendance_open_by_member", 1,
                statement -> statement.setLong(1, memberId), JdbcAttendanceRepository::toEntity).stream().findFirst();
    }

    @Override
    public AttendanceRecord save(AttendanceRecord value) {
        long id = JdbcSupport.save(connections, "pkg_nf_access.attendance_save", value.getId(), 3, statement -> {
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
