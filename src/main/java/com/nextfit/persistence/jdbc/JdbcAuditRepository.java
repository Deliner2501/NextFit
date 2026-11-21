package com.nextfit.persistence.jdbc;

import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.persistence.AuditRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stores the audit log in the {@code audit_log} table. Entries are only added, never changed.
 */
public final class JdbcAuditRepository implements AuditRepository {

    private static final String TABLE = "audit_log";
    private static final String[] DATA = {"event_time", "username", "action", "detail"};
    private static final String SELECT = "SELECT id, event_time, username, action, detail FROM audit_log";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcAuditRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<AuditEntry> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcAuditRepository::toEntity);
    }

    @Override
    public AuditEntry append(AuditEntry entry) {
        long id = JdbcSupport.insertOnly(connections, TABLE, entry.id(), DATA, statement -> {
            JdbcSupport.setTimestamp(statement, 1, entry.timestamp());
            statement.setString(2, entry.username());
            statement.setString(3, entry.action().name());
            statement.setString(4, entry.detail());
        });
        return new AuditEntry(id, entry.timestamp(), entry.username(), entry.action(), entry.detail());
    }

    private static AuditEntry toEntity(ResultSet rows) throws SQLException {
        return new AuditEntry(
                rows.getLong("id"),
                rows.getObject("event_time", LocalDateTime.class),
                JdbcSupport.emptyIfNull(rows.getString("username")),
                AuditAction.valueOf(rows.getString("action")),
                JdbcSupport.emptyIfNull(rows.getString("detail")));
    }
}
