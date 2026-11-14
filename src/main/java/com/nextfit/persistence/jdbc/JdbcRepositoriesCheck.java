package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AssignmentStatus;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.model.MembershipPlan;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Role;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.Subscription;
import com.nextfit.model.Trainer;
import com.nextfit.model.TrainingSession;
import com.nextfit.model.User;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Checks the JDBC repositories against the real database. It creates test rows
 * that depend on each other (a member with a plan, a subscription, a routine
 * assignment and so on), reads them back and then deletes everything, so the
 * database is left as it was.
 */
public final class JdbcRepositoriesCheck {

    private static int passed;
    private static int total;

    private JdbcRepositoriesCheck() {
    }

    /**
     * Runs the check from the folder that contains nextfit.properties.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        Optional<DatabaseConfig> config = DatabaseConfig.load(Path.of(DatabaseConfig.FILE_NAME));
        if (config.isEmpty()) {
            System.out.println("No existe " + DatabaseConfig.FILE_NAME + ".");
            return;
        }
        OracleConnectionProvider provider = new OracleConnectionProvider(config.get());
        Deque<String[]> cleanup = new ArrayDeque<>();
        try {
            run(provider, String.valueOf(System.currentTimeMillis()), cleanup);
        } catch (PersistenceException | SQLException e) {
            Throwable reason = e.getCause() != null ? e.getCause() : e;
            System.out.println("ERROR: " + e.getMessage() + " -> " + reason.getMessage());
        } finally {
            clean(provider, cleanup);
        }
        System.out.println("Resultado: " + passed + " de " + total + " comprobaciones correctas.");
    }

    private static void run(OracleConnectionProvider db, String unique, Deque<String[]> cleanup)
            throws SQLException {
        Member member = new JdbcMemberRepository(db).save(new Member(0, "Prueba Repos", "TEST" + unique,
                "3000000000", "p@nextfit.test", LocalDate.of(2000, 1, 1), LocalDate.now(), "", "", true));
        long memberId = member.getId();
        cleanup.push(new String[]{"members", "id", String.valueOf(memberId)});
        check("members.save", memberId != 0);

        JdbcUserRepository users = new JdbcUserRepository(db);
        User user = users.save(new User(0, "test" + unique, "Usuario Prueba", "hash", Role.values()[0], true));
        cleanup.push(new String[]{"users", "id", String.valueOf(user.getId())});
        check("users.findByUsername", users.findByUsername("TEST" + unique).isPresent());
        check("users.findById", users.findById(user.getId()).isPresent());

        JdbcMembershipPlanRepository plans = new JdbcMembershipPlanRepository(db);
        MembershipPlan plan = plans.save(new MembershipPlan(0, "Plan " + unique, "descripcion", 30, 50000L, true));
        cleanup.push(new String[]{"membership_plans", "id", String.valueOf(plan.getId())});
        check("plans.findByName", plans.findByName("plan " + unique).isPresent());
        check("plans.findById", plans.findById(plan.getId()).map(MembershipPlan::getPrice).orElse(0L) == 50000L);

        JdbcSubscriptionRepository subscriptions = new JdbcSubscriptionRepository(db);
        Subscription subscription = subscriptions.save(new Subscription(0, memberId, plan.getId(),
                LocalDate.now(), LocalDate.now().plusDays(30), false));
        cleanup.push(new String[]{"subscriptions", "id", String.valueOf(subscription.getId())});
        check("subscriptions.findByMemberId", subscriptions.findByMemberId(memberId).size() == 1);

        JdbcHealthProfileRepository health = new JdbcHealthProfileRepository(db);
        health.save(new HealthProfile(memberId, 170.5, "O+", "ninguna", "ninguna"));
        health.save(new HealthProfile(memberId, 180.0, "O+", "ninguna", "ninguna"));
        cleanup.push(new String[]{"health_profiles", "member_id", String.valueOf(memberId)});
        check("health.save (inserta y luego edita)",
                health.findByMemberId(memberId).map(HealthProfile::getHeightCm).orElse(0.0) == 180.0);

        JdbcMeasurementRepository measurements = new JdbcMeasurementRepository(db);
        Measurement measurement = measurements.save(
                new Measurement(0, memberId, LocalDate.now(), 70.5, 80.0, 95.0, 90.0));
        cleanup.push(new String[]{"measurements", "id", String.valueOf(measurement.getId())});
        check("measurements.findByMemberId", measurements.findByMemberId(memberId).stream()
                .anyMatch(m -> m.getWeightKg() == 70.5));

        JdbcAttendanceRepository attendance = new JdbcAttendanceRepository(db);
        LocalDateTime checkIn = LocalDateTime.now().withNano(0);
        AttendanceRecord visit = attendance.save(new AttendanceRecord(0, memberId, checkIn, null));
        cleanup.push(new String[]{"attendance", "id", String.valueOf(visit.getId())});
        check("attendance.findOpenByMemberId (dentro)", attendance.findOpenByMemberId(memberId).isPresent());
        attendance.save(new AttendanceRecord(visit.getId(), memberId, checkIn, checkIn.plusHours(1)));
        check("attendance.findOpenByMemberId (ya salió)", attendance.findOpenByMemberId(memberId).isEmpty());
        check("attendance.findByMemberId", attendance.findByMemberId(memberId).size() == 1);

        JdbcTrainerRepository trainers = new JdbcTrainerRepository(db);
        Trainer trainer = trainers.save(new Trainer(0, "Entrenador Prueba", "TR" + unique, "3000000001",
                "t@nextfit.test", LocalDate.of(1990, 1, 1), "Fuerza", "", true));
        cleanup.push(new String[]{"trainers", "id", String.valueOf(trainer.getId())});
        check("trainers.findByDocumentNumber", trainers.findByDocumentNumber("TR" + unique).isPresent());

        JdbcExerciseRepository exercises = new JdbcExerciseRepository(db);
        Exercise exercise = exercises.save(new Exercise(0, "Ejercicio " + unique, "descripcion",
                MuscleGroup.values()[0], Difficulty.values()[0], ""));
        cleanup.push(new String[]{"exercises", "id", String.valueOf(exercise.getId())});
        check("exercises.findByName", exercises.findByName("ejercicio " + unique).isPresent());

        long routineId = insertRoutine(db, "Rutina " + unique, trainer.getId());
        cleanup.push(new String[]{"routines", "id", String.valueOf(routineId)});

        JdbcAssignmentRepository assignments = new JdbcAssignmentRepository(db);
        RoutineAssignment assignment = assignments.save(new RoutineAssignment(0, memberId, routineId,
                LocalDate.now(), AssignmentStatus.values()[0]));
        cleanup.push(new String[]{"assignments", "id", String.valueOf(assignment.getId())});
        check("assignments.findByMemberId", assignments.findByMemberId(memberId).size() == 1);
        check("assignments.findById", assignments.findById(assignment.getId()).isPresent());

        JdbcTrainingSessionRepository sessions = new JdbcTrainingSessionRepository(db);
        TrainingSession session = sessions.save(new TrainingSession(0, memberId, assignment.getId(),
                "Dia 1", LocalDate.now(), 60, "nota"));
        cleanup.push(new String[]{"training_sessions", "id", String.valueOf(session.getId())});
        check("sessions.findByMemberId", sessions.findByMemberId(memberId).size() == 1);
        check("sessions.findById", sessions.findById(session.getId()).isPresent());

        JdbcAuditRepository audit = new JdbcAuditRepository(db);
        AuditEntry entry = audit.append(new AuditEntry(0, LocalDateTime.now(), "test" + unique,
                AuditAction.values()[0], "detalle"));
        cleanup.push(new String[]{"audit_log", "id", String.valueOf(entry.id())});
        check("audit.append y findAll", audit.findAll().stream().anyMatch(e -> e.id() == entry.id()));
    }

    private static long insertRoutine(OracleConnectionProvider db, String name, long trainerId) throws SQLException {
        String sql = "INSERT INTO routines (name, objective, trainer_id, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = db.open();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"ID"})) {
            statement.setString(1, name);
            statement.setString(2, "objetivo");
            statement.setLong(3, trainerId);
            statement.setObject(4, LocalDate.now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void clean(OracleConnectionProvider db, Deque<String[]> cleanup) {
        int deleted = 0;
        while (!cleanup.isEmpty()) {
            String[] item = cleanup.pop();
            String sql = "DELETE FROM " + item[0] + " WHERE " + item[1] + " = ?";
            try (Connection connection = db.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, Long.parseLong(item[2]));
                deleted += statement.executeUpdate();
            } catch (SQLException e) {
                System.out.println("No se pudo borrar de " + item[0] + ": " + e.getMessage());
            }
        }
        System.out.println("Datos de prueba borrados: " + deleted + " filas.");
    }

    private static void check(String name, boolean condition) {
        total++;
        if (condition) {
            passed++;
        }
        System.out.println((condition ? "OK     " : "FALLO  ") + name);
    }
}
