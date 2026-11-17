package com.nextfit.persistence.jdbc;

import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.model.Role;
import com.nextfit.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Users, the audit log and the migration between two storages, all on in-memory H2 databases. */
class JdbcUsersAuditAndMigrationTest {

    private H2Database source;
    private H2Database target;

    @BeforeEach
    void setUp() {
        source = new H2Database();
        target = new H2Database();
    }

    @AfterEach
    void tearDown() {
        source.close();
        target.close();
    }

    @Test
    void aUserIsFoundByNameIgnoringCaseAndEditedWithItsIdentifier() {
        JdbcUserRepository users = new JdbcUserRepository(source);
        User admin = users.save(new User(0, "admin", "Administrador", "hash", Role.values()[0], true));

        assertEquals(admin.getId(), users.findByUsername("ADMIN").orElseThrow().getId());
        users.save(new User(admin.getId(), "admin", "Administrador", "otro", Role.values()[0], false));

        assertEquals(1, users.findAll().size());
        assertEquals("otro", users.findById(admin.getId()).orElseThrow().getPasswordHash());
        assertTrue(!users.findById(admin.getId()).orElseThrow().isActive());
    }

    @Test
    void auditEntriesAreOnlyAddedAndKeepTheirOrder() {
        JdbcAuditRepository audit = new JdbcAuditRepository(source);
        AuditEntry first = audit.append(new AuditEntry(0, LocalDateTime.of(2026, 11, 1, 9, 0, 0), "admin",
                AuditAction.values()[0], "entro"));
        AuditEntry second = audit.append(new AuditEntry(0, LocalDateTime.of(2026, 11, 1, 9, 5, 0), "",
                AuditAction.values()[0], ""));

        List<AuditEntry> all = audit.findAll();

        assertEquals(2, all.size());
        assertEquals(first.id(), all.get(0).id());
        assertEquals("", all.get(1).username());
        assertEquals("", all.get(1).detail());
        assertTrue(second.id() > first.id());
    }

    @Test
    void theMigrationCopiesEverythingAndRunningItAgainRepeatsNothing() {
        populate(new JdbcStorage(source));
        DataMigration migration = new DataMigration(new JdbcStorage(source), new JdbcStorage(target));

        List<DataMigration.EntityResult> first = migration.run();
        List<DataMigration.EntityResult> second = migration.run();

        for (DataMigration.EntityResult result : first) {
            assertTrue(result.isComplete(), result.describe());
        }
        for (DataMigration.EntityResult result : second) {
            assertTrue(result.isComplete(), result.describe());
        }
        Map<String, Integer> expected = DataMigration.count(new JdbcStorage(source));
        assertEquals(expected, DataMigration.count(new JdbcStorage(target)));
        assertEquals(2, (int) expected.get("members"));
        assertEquals(1, (int) expected.get("member_credentials"));
        assertEquals(2, (int) expected.get("face_templates"));
    }

    @Test
    void theMigrationUpdatesAnAdminTheTargetAlreadyHasInsteadOfFailing() {
        new JdbcUserRepository(source).save(new User(7, "admin", "Admin del CSV", "clave-del-csv", Role.values()[0], true));
        JdbcUserRepository targetUsers = new JdbcUserRepository(target);
        User defaultAdmin = targetUsers.save(new User(0, "admin", "Admin por defecto", "clave-por-defecto", Role.values()[0], true));

        List<DataMigration.EntityResult> results = new DataMigration(new JdbcStorage(source), new JdbcStorage(target)).run();

        assertTrue(results.get(0).isComplete(), results.get(0).describe());
        assertEquals(1, targetUsers.findAll().size());
        User admin = targetUsers.findByUsername("admin").orElseThrow();
        assertEquals(defaultAdmin.getId(), admin.getId());
        assertEquals("clave-del-csv", admin.getPasswordHash());
    }

    @Test
    void theMigrationReportsABadRowAndKeepsCopyingTheRest() {
        JdbcStorage from = new JdbcStorage(source);
        from.members().save(TestData.member("100"));
        from.members().save(TestData.member("200"));
        // another member (id 99) already uses the document 200 in the target: copying member 200 must fail
        new JdbcStorage(target).members().save(new com.nextfit.model.Member(99, "Otro", "200", "", "",
                java.time.LocalDate.of(1990, 1, 1), java.time.LocalDate.of(2026, 1, 1), "", "", true));

        List<DataMigration.EntityResult> results = new DataMigration(from, new JdbcStorage(target)).run();

        DataMigration.EntityResult members = results.get(1);
        assertEquals("members", members.name());
        assertEquals(2, members.read());
        assertEquals(1, members.written());
        assertEquals(1, members.errors().size());
        assertTrue(!members.isComplete());
    }

    /** Two members with a plan, a payment, a routine, an assignment, a password, faces and an audit entry. */
    private static void populate(JdbcStorage storage) {
        long ana = storage.members().save(TestData.member("100")).getId();
        long luis = storage.members().save(TestData.member("200")).getId();
        storage.users().save(new User(0, "admin", "Admin", "hash", Role.values()[0], true));
        long plan = storage.plans().save(TestData.plan("Mensual")).getId();
        long subscription = storage.subscriptions().save(new com.nextfit.model.Subscription(0, ana, plan,
                java.time.LocalDate.of(2026, 11, 1), java.time.LocalDate.of(2026, 11, 30), false)).getId();
        storage.payments().save(new com.nextfit.model.Payment(0, subscription, ana,
                LocalDateTime.of(2026, 11, 1, 10, 0, 0), com.nextfit.model.PaymentMethod.values()[0], 50_000L,
                new com.nextfit.model.PercentageDiscount(10), "admin"));
        storage.attendance().save(new com.nextfit.model.AttendanceRecord(0, luis,
                LocalDateTime.of(2026, 11, 2, 8, 0, 0), null));
        storage.healthProfiles().save(new com.nextfit.model.HealthProfile(ana, 170.0, "O+", "", ""));
        storage.measurements().save(new com.nextfit.model.Measurement(0, ana,
                java.time.LocalDate.of(2026, 11, 1), 70.0, 80.0, 95.0, 90.0));
        long trainer = storage.trainers().save(TestData.trainer("T1")).getId();
        long exercise = storage.exercises().save(TestData.exercise("Sentadilla")).getId();
        long routine = storage.routines().save(new com.nextfit.model.Routine(0, "Fuerza", "obj", trainer,
                java.time.LocalDate.of(2026, 11, 1), List.of(new com.nextfit.model.RoutineDay("Lunes",
                List.of(new com.nextfit.model.RoutineExercise(exercise, 3, 10, 60)))))).getId();
        long assignment = storage.assignments().save(new com.nextfit.model.RoutineAssignment(0, ana, routine,
                java.time.LocalDate.of(2026, 11, 2), com.nextfit.model.AssignmentStatus.values()[0])).getId();
        storage.trainingSessions().save(new com.nextfit.model.TrainingSession(0, ana, assignment, "Lunes",
                java.time.LocalDate.of(2026, 11, 3), 45, "nota"));
        storage.memberCredentials().save(new com.nextfit.model.MemberCredential(ana, "hash-de-ana"));
        storage.faceTemplates().replace(ana, List.of(new float[]{1f, 2f}, new float[]{3f, 4f}));
        storage.audit().append(new AuditEntry(0, LocalDateTime.of(2026, 11, 1, 9, 0, 0), "admin",
                AuditAction.values()[0], "entro"));
    }
}
