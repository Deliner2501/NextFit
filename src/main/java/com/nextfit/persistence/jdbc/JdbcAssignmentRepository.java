package com.nextfit.persistence.jdbc;

import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.persistence.AssignmentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Stores the routines given to the members in the {@code assignments} table.
 */
public final class JdbcAssignmentRepository implements AssignmentRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcAssignmentRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<RoutineAssignment> findAll() {
        return JdbcSupport.query(connections, "pkg_nf_training.assignments_all", 0, null, JdbcAssignmentRepository::toEntity);
    }

    @Override
    public Optional<RoutineAssignment> findById(long id) {
        return JdbcSupport.query(connections, "pkg_nf_training.assignments_by_id", 1,
                statement -> statement.setLong(1, id), JdbcAssignmentRepository::toEntity).stream().findFirst();
    }

    @Override
    public List<RoutineAssignment> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, "pkg_nf_training.assignments_by_member", 1,
                statement -> statement.setLong(1, memberId), JdbcAssignmentRepository::toEntity);
    }

    @Override
    public RoutineAssignment save(RoutineAssignment value) {
        long id = JdbcSupport.save(connections, "pkg_nf_training.assignments_save", value.getId(), 4, statement -> {
            statement.setLong(1, value.getMemberId());
            statement.setLong(2, value.getRoutineId());
            statement.setObject(3, value.getAssignedAt());
            statement.setString(4, value.getStatus().name());
        });
        return new RoutineAssignment(id, value.getMemberId(), value.getRoutineId(), value.getAssignedAt(), value.getStatus());
    }

    private static RoutineAssignment toEntity(ResultSet rows) throws SQLException {
        return new RoutineAssignment(
                rows.getLong("id"),
                rows.getLong("member_id"),
                rows.getLong("routine_id"),
                rows.getObject("assigned_at", LocalDate.class),
                AssignmentStatus.valueOf(rows.getString("status")));
    }
}
