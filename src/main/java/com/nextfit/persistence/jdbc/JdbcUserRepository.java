package com.nextfit.persistence.jdbc;

import com.nextfit.model.Role;
import com.nextfit.model.User;
import com.nextfit.persistence.UserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Stores users in the {@code users} table.
 */
public final class JdbcUserRepository implements UserRepository {

    private static final String TABLE = "users";
    private static final String[] DATA = {"username", "full_name", "password_hash", "role", "active"};
    private static final String SELECT = "SELECT id, username, full_name, password_hash, role, active FROM users";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcUserRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<User> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcUserRepository::toEntity);
    }

    @Override
    public Optional<User> findById(long id) {
        return JdbcSupport.query(connections, SELECT + " WHERE id = ?",
                statement -> statement.setLong(1, id), JdbcUserRepository::toEntity).stream().findFirst();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return JdbcSupport.query(connections, SELECT + " WHERE LOWER(username) = LOWER(?)",
                statement -> statement.setString(1, username), JdbcUserRepository::toEntity).stream().findFirst();
    }

    @Override
    public User save(User value) {
        long id = JdbcSupport.save(connections, TABLE, value.getId(), DATA, statement -> {
            statement.setString(1, value.getUsername());
            statement.setString(2, value.getFullName());
            statement.setString(3, value.getPasswordHash());
            statement.setString(4, value.getRole().name());
            statement.setInt(5, value.isActive() ? 1 : 0);
        });
        return new User(id, value.getUsername(), value.getFullName(), value.getPasswordHash(), value.getRole(), value.isActive());
    }

    private static User toEntity(ResultSet rows) throws SQLException {
        return new User(
                rows.getLong("id"),
                rows.getString("username"),
                rows.getString("full_name"),
                rows.getString("password_hash"),
                Role.valueOf(rows.getString("role")),
                rows.getInt("active") == 1);
    }
}
