package com.nextfit.persistence.jdbc;

import com.nextfit.model.TrainingSession;
import com.nextfit.persistence.TrainingSessionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Stores the workouts done by the members in the {@code training_sessions} table.
 */
public final class JdbcTrainingSessionRepository implements TrainingSessionRepository {

    private static final String TABLE = "training_sessions";
    private static final String[] DATA = {"member_id", "assignment_id", "day_name", "session_date", "duration_minutes", "notes"};
    private static final String SELECT = "SELECT id, member_id, assignment_id, day_name, session_date, duration_minutes, notes FROM training_sessions";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcTrainingSessionRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<TrainingSession> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcTrainingSessionRepository::toEntity);
    }

    @Override
    public Optional<TrainingSession> findById(long id) {
        return JdbcSupport.query(connections, SELECT + " WHERE id = ?",
                statement -> statement.setLong(1, id), JdbcTrainingSessionRepository::toEntity).stream().findFirst();
    }

    @Override
    public List<TrainingSession> findByMemberId(long memberId) {
        return JdbcSupport.query(connections, SELECT + " WHERE member_id = ? ORDER BY id",
                statement -> statement.setLong(1, memberId), JdbcTrainingSessionRepository::toEntity);
    }

    @Override
    public TrainingSession save(TrainingSession value) {
        long id = JdbcSupport.save(connections, TABLE, value.getId(), DATA, statement -> {
            statement.setLong(1, value.getMemberId());
            statement.setLong(2, value.getAssignmentId());
            statement.setString(3, value.getDayName());
            statement.setObject(4, value.getDate());
            statement.setInt(5, value.getDurationMinutes());
            statement.setString(6, value.getNotes());
        });
        return new TrainingSession(id, value.getMemberId(), value.getAssignmentId(), value.getDayName(), value.getDate(), value.getDurationMinutes(), value.getNotes());
    }

    private static TrainingSession toEntity(ResultSet rows) throws SQLException {
        return new TrainingSession(
                rows.getLong("id"),
                rows.getLong("member_id"),
                rows.getLong("assignment_id"),
                JdbcSupport.emptyIfNull(rows.getString("day_name")),
                rows.getObject("session_date", LocalDate.class),
                rows.getInt("duration_minutes"),
                JdbcSupport.emptyIfNull(rows.getString("notes")));
    }
}
