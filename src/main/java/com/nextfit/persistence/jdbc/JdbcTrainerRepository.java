package com.nextfit.persistence.jdbc;

import com.nextfit.model.Trainer;
import com.nextfit.persistence.TrainerRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Stores trainers in the {@code trainers} table.
 */
public final class JdbcTrainerRepository implements TrainerRepository {

    private static final String TABLE = "trainers";
    private static final String[] DATA = {"full_name", "document_number", "phone", "email", "birth_date", "specialty", "photo_path", "active"};
    private static final String SELECT = "SELECT id, full_name, document_number, phone, email, birth_date, specialty, photo_path, active FROM trainers";

    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcTrainerRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Trainer> findAll() {
        return JdbcSupport.query(connections, SELECT + " ORDER BY id", null, JdbcTrainerRepository::toEntity);
    }

    @Override
    public Optional<Trainer> findById(long id) {
        return JdbcSupport.query(connections, SELECT + " WHERE id = ?",
                statement -> statement.setLong(1, id), JdbcTrainerRepository::toEntity).stream().findFirst();
    }

    @Override
    public Optional<Trainer> findByDocumentNumber(String documentNumber) {
        return JdbcSupport.query(connections, SELECT + " WHERE document_number = ?",
                statement -> statement.setString(1, documentNumber), JdbcTrainerRepository::toEntity).stream().findFirst();
    }

    @Override
    public Trainer save(Trainer value) {
        long id = JdbcSupport.save(connections, TABLE, value.getId(), DATA, statement -> {
            statement.setString(1, value.getFullName());
            statement.setString(2, value.getDocumentNumber());
            statement.setString(3, value.getPhone());
            statement.setString(4, value.getEmail());
            statement.setObject(5, value.getBirthDate());
            statement.setString(6, value.getSpecialty());
            statement.setString(7, value.getPhotoPath());
            statement.setInt(8, value.isActive() ? 1 : 0);
        });
        return new Trainer(id, value.getFullName(), value.getDocumentNumber(), value.getPhone(), value.getEmail(), value.getBirthDate(), value.getSpecialty(), value.getPhotoPath(), value.isActive());
    }

    private static Trainer toEntity(ResultSet rows) throws SQLException {
        return new Trainer(
                rows.getLong("id"),
                rows.getString("full_name"),
                rows.getString("document_number"),
                JdbcSupport.emptyIfNull(rows.getString("phone")),
                JdbcSupport.emptyIfNull(rows.getString("email")),
                rows.getObject("birth_date", LocalDate.class),
                JdbcSupport.emptyIfNull(rows.getString("specialty")),
                JdbcSupport.emptyIfNull(rows.getString("photo_path")),
                rows.getInt("active") == 1);
    }
}
