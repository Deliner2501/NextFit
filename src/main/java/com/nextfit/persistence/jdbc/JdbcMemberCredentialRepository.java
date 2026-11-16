package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.MemberCredential;
import com.nextfit.persistence.MemberCredentialRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Stores the password hash of each member in the {@code member_credentials} table.
 * There is one row per member, so the member id is the key. Only the hash is kept,
 * never the password.
 */
public final class JdbcMemberCredentialRepository implements MemberCredentialRepository {

    private static final String SELECT =
            "SELECT member_id, password_hash FROM member_credentials WHERE member_id = ?";
    private static final String UPDATE =
            "UPDATE member_credentials SET password_hash = ? WHERE member_id = ?";
    private static final String INSERT =
            "INSERT INTO member_credentials (password_hash, member_id) VALUES (?, ?)";
    private static final String DELETE = "DELETE FROM member_credentials WHERE member_id = ?";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcMemberCredentialRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public Optional<MemberCredential> find(long memberId) {
        return JdbcSupport.query(connections, SELECT, statement -> statement.setLong(1, memberId),
                rows -> new MemberCredential(rows.getLong("member_id"), rows.getString("password_hash")))
                .stream().findFirst();
    }

    /** Replaces the credential the member had: it updates the row and inserts it when there was none. */
    @Override
    public void save(MemberCredential credential) {
        try (Connection connection = connections.open()) {
            if (write(connection, UPDATE, credential) == 0) {
                write(connection, INSERT, credential);
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar la contraseña del socio en la base de datos", e);
        }
    }

    @Override
    public void delete(long memberId) {
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setLong(1, memberId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo borrar la contraseña del socio de la base de datos", e);
        }
    }

    private static int write(Connection connection, String sql, MemberCredential credential) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, credential.passwordHash());
            statement.setLong(2, credential.memberId());
            return statement.executeUpdate();
        }
    }
}
