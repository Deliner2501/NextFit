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

    private static final String COLUMNS =
            "id, full_name, document_number, phone, email, birth_date, join_date, "
                    + "photo_path, emergency_contact, active";

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
        return query("SELECT " + COLUMNS + " FROM members ORDER BY id", null);
    }

    @Override
    public Optional<Member> findById(long id) {
        return query("SELECT " + COLUMNS + " FROM members WHERE id = ?",
                statement -> statement.setLong(1, id)).stream().findFirst();
    }

    @Override
    public Optional<Member> findByDocumentNumber(String documentNumber) {
        return query("SELECT " + COLUMNS + " FROM members WHERE document_number = ?",
                statement -> statement.setString(1, documentNumber)).stream().findFirst();
    }

    @Override
    public Member save(Member member) {
        try (Connection connection = connections.open()) {
            if (member.getId() != 0 && update(connection, member) > 0) {
                return member;
            }
            return insert(connection, member);
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar el socio en la base de datos", e);
        }
    }

    private int update(Connection connection, Member member) throws SQLException {
        String sql = "UPDATE members SET full_name = ?, document_number = ?, phone = ?, email = ?, "
                + "birth_date = ?, join_date = ?, photo_path = ?, emergency_contact = ?, active = ? "
                + "WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindData(statement, member, 1);
            statement.setLong(10, member.getId());
            return statement.executeUpdate();
        }
    }

    /** Inserts with the given id (migration) or lets the database assign one (id 0). */
    private Member insert(Connection connection, Member member) throws SQLException {
        boolean hasId = member.getId() != 0;
        String sql = hasId
                ? "INSERT INTO members (full_name, document_number, phone, email, birth_date, join_date, "
                + "photo_path, emergency_contact, active, id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO members (full_name, document_number, phone, email, birth_date, join_date, "
                + "photo_path, emergency_contact, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, new String[]{"ID"})) {
            bindData(statement, member, 1);
            if (hasId) {
                statement.setLong(10, member.getId());
            }
            statement.executeUpdate();
            if (hasId) {
                return member;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new PersistenceException("La base de datos no devolvió el identificador del socio", null);
                }
                return member.withId(keys.getLong(1));
            }
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

    private List<Member> query(String sql, StatementBinder binder) {
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(statement);
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<Member> members = new ArrayList<>();
                while (rows.next()) {
                    members.add(toMember(rows));
                }
                return members;
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo leer los socios de la base de datos", e);
        }
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

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
