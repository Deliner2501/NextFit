package com.nextfit.persistence.jdbc;

import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.persistence.ExerciseRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Stores the exercise catalog in the {@code exercises} table.
 */
public final class JdbcExerciseRepository implements ExerciseRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcExerciseRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Exercise> findAll() {
        return JdbcSupport.query(connections, "pkg_nf_training.exercises_all", 0, null, JdbcExerciseRepository::toEntity);
    }

    @Override
    public Optional<Exercise> findById(long id) {
        return JdbcSupport.query(connections, "pkg_nf_training.exercises_by_id", 1,
                statement -> statement.setLong(1, id), JdbcExerciseRepository::toEntity).stream().findFirst();
    }

    @Override
    public Optional<Exercise> findByName(String name) {
        return JdbcSupport.query(connections, "pkg_nf_training.exercises_by_name", 1,
                statement -> statement.setString(1, name), JdbcExerciseRepository::toEntity).stream().findFirst();
    }

    @Override
    public Exercise save(Exercise value) {
        long id = JdbcSupport.save(connections, "pkg_nf_training.exercises_save", value.getId(), 5, statement -> {
            statement.setString(1, value.getName());
            statement.setString(2, value.getDescription());
            statement.setString(3, value.getMuscleGroup().name());
            statement.setString(4, value.getDifficulty().name());
            statement.setString(5, value.getImagePath());
        });
        return new Exercise(id, value.getName(), value.getDescription(), value.getMuscleGroup(), value.getDifficulty(), value.getImagePath());
    }

    private static Exercise toEntity(ResultSet rows) throws SQLException {
        return new Exercise(
                rows.getLong("id"),
                rows.getString("name"),
                JdbcSupport.emptyIfNull(rows.getString("description")),
                MuscleGroup.valueOf(rows.getString("muscle_group")),
                Difficulty.valueOf(rows.getString("difficulty")),
                JdbcSupport.emptyIfNull(rows.getString("image_path")));
    }
}
