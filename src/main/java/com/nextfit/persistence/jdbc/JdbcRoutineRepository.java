package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.persistence.RoutineRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Stores routines in two tables: {@code routines} (one row per routine) and
 * {@code routine_lines} (one row per exercise of each day). A routine and its
 * lines are always saved together in one transaction.
 */
public final class JdbcRoutineRepository implements RoutineRepository {


    private final ConnectionSource connections;

    /**
     * Creates the repository.
     *
     * @param connections where the connections come from
     */
    public JdbcRoutineRepository(ConnectionSource connections) {
        this.connections = connections;
    }

    @Override
    public List<Routine> findAll() {
        return load("all", 0, null);
    }

    @Override
    public Optional<Routine> findById(long id) {
        return load("by_id", 1, statement -> statement.setLong(1, id)).stream().findFirst();
    }

    @Override
    public List<Routine> findByTrainerId(long trainerId) {
        return load("by_trainer", 1, statement -> statement.setLong(1, trainerId));
    }

    /** Saves the routine and replaces all its lines, in one transaction. */
    @Override
    public Routine save(Routine routine) {
        try (Connection connection = connections.open()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                long id = JdbcSupport.save(connection, "pkg_nf_training.routines_save", routine.getId(), 4, statement -> {
                    statement.setString(1, routine.getName());
                    statement.setString(2, routine.getObjective());
                    if (routine.getTrainerId() == 0) {
                        statement.setNull(3, Types.NUMERIC);
                    } else {
                        statement.setLong(3, routine.getTrainerId());
                    }
                    statement.setObject(4, routine.getCreatedAt());
                });
                replaceLines(connection, id, routine);
                connection.commit();
                return routine.withId(id);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new PersistenceException("No se pudo guardar la rutina en la base de datos", e);
        }
    }

    private static void replaceLines(Connection connection, long routineId, Routine routine) throws SQLException {
        try (PreparedStatement delete = JdbcSupport.prepare(connection, "pkg_nf_training.lines_delete", 1)) {
            delete.setLong(1, routineId);
            delete.execute();
        }
        try (PreparedStatement insert = JdbcSupport.prepare(connection, "pkg_nf_training.lines_insert", 8)) {
            List<RoutineDay> days = routine.getDays();
            for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
                RoutineDay day = days.get(dayIndex);
                int lineNo = 0;
                for (RoutineExercise line : day.getExercises()) {
                    insert.setLong(1, routineId);
                    insert.setInt(2, dayIndex);
                    insert.setInt(3, lineNo++);
                    insert.setString(4, day.getName());
                    insert.setLong(5, line.getExerciseId());
                    insert.setInt(6, line.getSets());
                    insert.setInt(7, line.getRepetitions());
                    insert.setInt(8, line.getRestSeconds());
                    insert.execute();
                }
            }
        }
    }

    /** Reads the routines that match the condition and then their lines, with two queries. */
    private List<Routine> load(String operation, int inputs, JdbcSupport.StatementBinder binder) {
        List<RoutineRow> routines = JdbcSupport.query(connections,
                "pkg_nf_training.routines_" + operation, inputs, binder, JdbcRoutineRepository::toRoutineRow);
        if (routines.isEmpty()) {
            return new ArrayList<>();
        }
        List<LineRow> lines = JdbcSupport.query(connections,
                "pkg_nf_training.lines_" + operation, inputs, binder, JdbcRoutineRepository::toLineRow);
        Map<Long, Map<Integer, DayDraft>> draftsByRoutine = new HashMap<>();
        for (LineRow line : lines) {
            draftsByRoutine.computeIfAbsent(line.routineId(), key -> new TreeMap<>())
                    .computeIfAbsent(line.dayIndex(), key -> new DayDraft(line.dayName()))
                    .exercises().add(line.exercise());
        }
        List<Routine> result = new ArrayList<>();
        for (RoutineRow row : routines) {
            List<RoutineDay> days = new ArrayList<>();
            for (DayDraft draft : draftsByRoutine.getOrDefault(row.id(), Map.of()).values()) {
                days.add(new RoutineDay(draft.name(), draft.exercises()));
            }
            result.add(new Routine(row.id(), row.name(), row.objective(), row.trainerId(), row.createdAt(), days));
        }
        return result;
    }

    private static RoutineRow toRoutineRow(ResultSet rows) throws SQLException {
        return new RoutineRow(
                rows.getLong("id"),
                rows.getString("name"),
                JdbcSupport.emptyIfNull(rows.getString("objective")),
                rows.getLong("trainer_id"),
                rows.getObject("created_at", LocalDate.class));
    }

    private static LineRow toLineRow(ResultSet rows) throws SQLException {
        return new LineRow(
                rows.getLong("routine_id"),
                rows.getInt("day_index"),
                JdbcSupport.emptyIfNull(rows.getString("day_name")),
                new RoutineExercise(rows.getLong("exercise_id"), rows.getInt("sets"),
                        rows.getInt("repetitions"), rows.getInt("rest_seconds")));
    }

    /** A routine row without its days. A missing trainer (NULL) is read as 0. */
    private record RoutineRow(long id, String name, String objective, long trainerId, LocalDate createdAt) {
    }

    private record LineRow(long routineId, int dayIndex, String dayName, RoutineExercise exercise) {
    }

    private record DayDraft(String name, List<RoutineExercise> exercises) {
        DayDraft(String name) {
            this(name, new ArrayList<>());
        }
    }
}
