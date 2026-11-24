package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Member;
import com.nextfit.persistence.MemberRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stores members in the {@code members} table. It is the database version of
 * the CSV repository: the services do not notice the difference.
 */
public final class JdbcMemberRepository implements MemberRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcMemberRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Member> findAll() {
        return JdbcSupport.query(connections, "pkg_nf_members.members_all", 0, null, JdbcMemberRepository::toMember);
    }

    @Override
    public Optional<Member> findById(long id) {
        return JdbcSupport.query(connections, "pkg_nf_members.members_by_id", 1,
                statement -> statement.setLong(1, id), JdbcMemberRepository::toMember).stream().findFirst();
    }

    @Override
    public Optional<Member> findByDocumentNumber(String documentNumber) {
        return JdbcSupport.query(connections, "pkg_nf_members.members_by_document", 1,
                statement -> statement.setString(1, documentNumber), JdbcMemberRepository::toMember).stream().findFirst();
    }

    @Override
    public Member save(Member member) {
        try {
            long id = JdbcSupport.save(connections, "pkg_nf_members.members_save", member.getId(), 9,
                    statement -> bindData(statement, member, 1));
            return member.withId(id);
        } catch (PersistenceException e) {
            throw new PersistenceException("No se pudo guardar el socio en la base de datos", e.getCause());
        }
    }

    private void bindData(PreparedStatement statement, Member member, int first) throws SQLException {
        statement.setString(first, member.getFullName());
        statement.setString(first + 1, member.getDocumentNumber());
        statement.setString(first + 2, member.getPhone());
        statement.setString(first + 3, member.getEmail());
        statement.setObject(first + 4, member.getBirthDate());
        statement.setObject(first + 5, member.getJoinDate());
        statement.setString(first + 6, member.getPhotoPath());
        statement.setString(first + 7, member.getEmergencyContact());
        statement.setInt(first + 8, member.isActive() ? 1 : 0);
    }

    /** Oracle stores an empty text as NULL, so NULL is turned back into "". */
    private static Member toMember(ResultSet rows) throws SQLException {
        return new Member(
                rows.getLong("id"),
                rows.getString("full_name"),
                rows.getString("document_number"),
                emptyIfNull(rows.getString("phone")),
                emptyIfNull(rows.getString("email")),
                rows.getObject("birth_date", LocalDate.class),
                rows.getObject("join_date", LocalDate.class),
                emptyIfNull(rows.getString("photo_path")),
                emptyIfNull(rows.getString("emergency_contact")),
                rows.getInt("active") == 1);
    }

    private static String emptyIfNull(String text) {
        return text == null ? "" : text;
    }

}
