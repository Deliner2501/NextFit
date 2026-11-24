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
        return JdbcSupport.query(connections, "pkg_nf_access.member_credentials_by_member", 1, statement -> statement.setLong(1, memberId),
                rows -> new MemberCredential(rows.getLong("member_id"), rows.getString("password_hash")))
                .stream().findFirst();
    }

    /** Replaces the credential the member had: it updates the row and inserts it when there was none. */
    @Override
    public java.util.Set<Long> credentialIds(java.util.List<Long> members) {
        return java.util.Set.copyOf(JdbcSupport.query(connections, "pkg_nf_access.member_credentials_ids", 0,
                null, rows -> rows.getLong(1)));
    }

    @Override
    public void save(MemberCredential credential) {
        JdbcSupport.save(connections, "pkg_nf_access.member_credentials_save", credential.memberId(), 1,
                statement -> statement.setString(1, credential.passwordHash()));
    }

    @Override
    public void delete(long memberId) {
        JdbcSupport.execute(connections, "pkg_nf_access.member_credentials_delete", 1,
                statement -> statement.setLong(1, memberId));
    }
}
