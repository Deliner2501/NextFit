package com.nextfit.persistence.jdbc;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.model.MemberCredential;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Members and everything that hangs from a member, on an in-memory H2 database. */
class JdbcMemberSideRepositoriesTest {

    private H2Database db;
    private JdbcMemberRepository members;
    private long memberId;

    @BeforeEach
    void setUp() {
        db = new H2Database();
        members = new JdbcMemberRepository(db);
        memberId = members.save(TestData.member("100")).getId();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void aNewMemberGetsAnIdentifierAndCanBeFoundByIdAndByDocument() {
        assertTrue(memberId > 0);
        assertEquals("Socio 100", members.findById(memberId).orElseThrow().getFullName());
        assertEquals(memberId, members.findByDocumentNumber("100").orElseThrow().getId());
        assertTrue(members.findById(memberId + 99).isEmpty());
    }

    @Test
    void savingAMemberWithItsIdentifierEditsItInsteadOfRepeatingIt() {
        Member member = members.findById(memberId).orElseThrow();

        members.save(new Member(memberId, "Nuevo Nombre", member.getDocumentNumber(), member.getPhone(),
                member.getEmail(), member.getBirthDate(), member.getJoinDate(), "", "", false));

        assertEquals(1, members.findAll().size());
        Member edited = members.findById(memberId).orElseThrow();
        assertEquals("Nuevo Nombre", edited.getFullName());
        assertFalse(edited.isActive());
    }

    @Test
    void aMemberSavedWithAGivenIdentifierKeepsIt() {
        Member copied = members.save(new Member(500, "Copiado", "500", "", "", LocalDate.of(1999, 5, 5),
                LocalDate.of(2026, 2, 2), "", "", true));

        assertEquals(500, copied.getId());
        assertEquals("Copiado", members.findById(500).orElseThrow().getFullName());
    }

    @Test
    void emptyTextsComeBackAsEmptyTextsEvenThoughTheDatabaseStoresNull() {
        Member read = members.findById(memberId).orElseThrow();

        assertEquals("", read.getPhotoPath());
        assertEquals("", read.getEmergencyContact());
    }

    @Test
    void aRepeatedDocumentIsRefusedWithAPersistenceException() {
        PersistenceException error = assertThrows(PersistenceException.class,
                () -> members.save(TestData.member("100")));

        assertTrue(error.getMessage().contains("socio"));
    }

    @Test
    void healthProfileIsInsertedTheFirstTimeAndEditedAfterwards() {
        JdbcHealthProfileRepository health = new JdbcHealthProfileRepository(db);
        assertTrue(health.findByMemberId(memberId).isEmpty());

        health.save(new HealthProfile(memberId, 170.5, "O+", "ninguna", ""));
        health.save(new HealthProfile(memberId, 180.0, "A-", "asma", "polen"));

        HealthProfile read = health.findByMemberId(memberId).orElseThrow();
        assertEquals(180.0, read.getHeightCm(), 0.001);
        assertEquals("A-", read.getBloodType());
        assertEquals("polen", read.getAllergies());
    }

    @Test
    void measurementsAreListedByMemberInDateOrder() {
        JdbcMeasurementRepository measurements = new JdbcMeasurementRepository(db);
        measurements.save(new Measurement(0, memberId, LocalDate.of(2026, 3, 1), 72.0, 80.0, 95.0, 90.0));
        measurements.save(new Measurement(0, memberId, LocalDate.of(2026, 1, 1), 75.5, 82.0, 96.0, 91.0));

        List<Measurement> read = measurements.findByMemberId(memberId);

        assertEquals(2, read.size());
        assertEquals(75.5, read.get(0).getWeightKg(), 0.001);
        assertEquals(72.0, read.get(1).getWeightKg(), 0.001);
        assertTrue(measurements.findByMemberId(memberId + 99).isEmpty());
    }

    @Test
    void aVisitIsOpenUntilItGetsAnExitTime() {
        JdbcAttendanceRepository attendance = new JdbcAttendanceRepository(db);
        LocalDateTime checkIn = LocalDateTime.of(2026, 11, 10, 18, 30, 0);

        AttendanceRecord visit = attendance.save(new AttendanceRecord(0, memberId, checkIn, null));
        assertEquals(visit.getId(), attendance.findOpenByMemberId(memberId).orElseThrow().getId());

        attendance.save(new AttendanceRecord(visit.getId(), memberId, checkIn, checkIn.plusHours(1)));

        assertTrue(attendance.findOpenByMemberId(memberId).isEmpty());
        AttendanceRecord closed = attendance.findByMemberId(memberId).get(0);
        assertEquals(checkIn.plusHours(1), closed.getCheckOut().orElseThrow());
        assertEquals(1, attendance.findAll().size());
    }

    @Test
    void theOpenVisitIsTheLatestOneWhenThereAreSeveral() {
        JdbcAttendanceRepository attendance = new JdbcAttendanceRepository(db);
        attendance.save(new AttendanceRecord(0, memberId, LocalDateTime.of(2026, 11, 1, 8, 0, 0), null));
        AttendanceRecord latest = attendance.save(
                new AttendanceRecord(0, memberId, LocalDateTime.of(2026, 11, 2, 8, 0, 0), null));

        assertEquals(latest.getId(), attendance.findOpenByMemberId(memberId).orElseThrow().getId());
    }

    @Test
    void aMemberHasOnePasswordAndSavingAgainReplacesIt() {
        JdbcMemberCredentialRepository credentials = new JdbcMemberCredentialRepository(db);
        assertTrue(credentials.find(memberId).isEmpty());

        credentials.save(new MemberCredential(memberId, "PBKDF2$1000$salt$first"));
        credentials.save(new MemberCredential(memberId, "PBKDF2$1000$salt$second"));

        assertEquals("PBKDF2$1000$salt$second", credentials.find(memberId).orElseThrow().passwordHash());
        credentials.delete(memberId);
        credentials.delete(memberId);
        assertTrue(credentials.find(memberId).isEmpty());
    }

    @Test
    void facesAreReplacedAsAWholeAndKeepTheirNumbers() {
        JdbcFaceTemplateRepository faces = new JdbcFaceTemplateRepository(db);
        float[] first = new float[128];
        float[] second = new float[128];
        for (int i = 0; i < 128; i++) {
            first[i] = i / 10f;
            second[i] = -i / 7f;
        }

        faces.replace(memberId, List.of(first, second));
        assertEquals(2, faces.findAll().stream().filter(t -> t.memberId() == memberId).count());
        assertTrue(faces.has(memberId));
        assertTrue(java.util.Arrays.equals(first, faces.findAll().get(0).vector()));

        faces.replace(memberId, List.of(second));
        assertEquals(1, faces.findAll().size());

        faces.delete(memberId);
        assertFalse(faces.has(memberId));
    }

    @Test
    void thePasswordAndTheFacesAreDeletedTogetherWithTheMember() throws Exception {
        new JdbcMemberCredentialRepository(db).save(new MemberCredential(memberId, "hash"));
        new JdbcFaceTemplateRepository(db).replace(memberId, List.of(new float[]{1f, 2f}));

        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM members WHERE id = " + memberId);
        }

        assertTrue(new JdbcMemberCredentialRepository(db).find(memberId).isEmpty());
        assertFalse(new JdbcFaceTemplateRepository(db).has(memberId));
    }
}
