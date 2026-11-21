package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.AuditAction;
import com.nextfit.model.AuditEntry;
import com.nextfit.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Saves objects with text that breaks naive CSV code and reads them back from a new repository. */
class CsvRoundTripTest {

    @TempDir
    Path folder;

    private static Member member(String name, String emergency) {
        return new Member(0, name, "1065123456", "3001112233", "ana@example.com", LocalDate.of(1998, 5, 1),
                LocalDate.of(2026, 1, 5), "data/photos/ana.jpg", emergency, true);
    }

    @Test
    void aMemberWithHostileTextIsReadBackIdentical() {
        Path file = folder.resolve("members.csv");
        Member saved = new CsvMemberRepository(file).save(
                member("Ana \"Anita\" Pérez, hija de Luis", "Mamá, 3001112233\nllamar de día"));

        Member read = new CsvMemberRepository(file).findById(saved.getId()).orElseThrow();

        assertEquals("Ana \"Anita\" Pérez, hija de Luis", read.getFullName());
        assertEquals("Mamá, 3001112233\nllamar de día", read.getEmergencyContact());
        assertEquals(LocalDate.of(1998, 5, 1), read.getBirthDate());
        assertEquals(LocalDate.of(2026, 1, 5), read.getJoinDate());
        assertEquals("data/photos/ana.jpg", read.getPhotoPath());
        assertTrue(read.isActive());
    }

    @Test
    void identifiersGrowAndUpdatesReplaceTheSameRow() {
        Path file = folder.resolve("members.csv");
        CsvMemberRepository repository = new CsvMemberRepository(file);
        Member first = repository.save(member("Primero", ""));
        Member second = repository.save(member("Segundo", ""));
        repository.save(first.withId(first.getId()));

        List<Member> all = new CsvMemberRepository(file).findAll();

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(2, all.size());
    }

    @Test
    void theAuditLogKeepsItsEntriesAcrossRestarts() {
        Path file = folder.resolve("audit.csv");
        CsvAuditRepository first = new CsvAuditRepository(file);
        LocalDateTime moment = LocalDateTime.of(2026, 11, 10, 21, 30, 15);
        first.append(new AuditEntry(0, moment, "admin", AuditAction.LOGIN, "Inició sesión"));
        first.append(new AuditEntry(0, moment.plusMinutes(1), "admin", AuditAction.LOGOUT, "Con coma, \"comillas\""));

        List<AuditEntry> read = new CsvAuditRepository(file).findAll();

        assertEquals(2, read.size());
        assertEquals(1, read.get(0).id());
        assertEquals(2, read.get(1).id());
        assertEquals(moment, read.get(0).timestamp());
        assertEquals(AuditAction.LOGOUT, read.get(1).action());
        assertEquals("Con coma, \"comillas\"", read.get(1).detail());
    }

    @Test
    void aCorruptAuditRowIsReportedInsteadOfIgnored() throws IOException {
        Path file = folder.resolve("audit.csv");
        Files.writeString(file, "id,timestamp,username,action,detail\nx,not-a-date,admin,NOPE,\n", StandardCharsets.UTF_8);

        assertThrows(PersistenceException.class, () -> new CsvAuditRepository(file).findAll());
    }
}
