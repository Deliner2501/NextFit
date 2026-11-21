package com.nextfit.service;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupServiceTest {

    @TempDir
    Path temp;

    private Path data;
    private Path backups;
    private BackupService service;

    @BeforeEach
    void setUp() throws IOException {
        data = temp.resolve("data");
        backups = temp.resolve("backups");
        Files.createDirectories(data.resolve("images/exercises"));
        Files.writeString(data.resolve("users.csv"), "id,username\n1,admin\n", StandardCharsets.UTF_8);
        Files.writeString(data.resolve("members.csv"), "id,name\n1,Ana\n", StandardCharsets.UTF_8);
        Files.writeString(data.resolve("access.key"), "secret", StandardCharsets.UTF_8);
        Files.write(data.resolve("images/exercises/press.png"), new byte[] {1, 2, 3});
        Files.writeString(data.resolve("notes.txt"), "not part of a backup", StandardCharsets.UTF_8);
        service = new BackupService(data, backups, clockAt("2026-11-08T15:00:00Z"));
    }

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private static List<String> names(Path zip) throws IOException {
        try (var file = new java.util.zip.ZipFile(zip.toFile())) {
            return file.stream().map(ZipEntry::getName).sorted().toList();
        }
    }

    @Test
    void aBackupHoldsTheDataFilesAndNothingElse() throws IOException {
        Path zip = service.createBackup();

        assertEquals(List.of("access.key", "images/exercises/press.png", "members.csv", "users.csv"), names(zip));
        assertEquals(1, service.list().size());
    }

    @Test
    void restoringBringsBackTheOldDataAndKeepsASafetyCopy() throws IOException {
        Path zip = service.createBackup();
        Files.writeString(data.resolve("members.csv"), "id,name\n1,Ana\n2,Beto\n", StandardCharsets.UTF_8);
        Files.delete(data.resolve("images/exercises/press.png"));

        Path safety = service.restore(zip);

        assertEquals("id,name\n1,Ana\n", Files.readString(data.resolve("members.csv")));
        assertTrue(Files.exists(data.resolve("images/exercises/press.png")));
        assertTrue(Files.exists(safety));
        try (var file = new java.util.zip.ZipFile(safety.toFile())) {
            String old = new String(file.getInputStream(file.getEntry("members.csv")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(old.contains("Beto"));
        }
    }

    @Test
    void aZipThatTriesToLeaveTheDataFolderIsRefused() throws IOException {
        Path evil = temp.resolve("evil.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(evil))) {
            out.putNextEntry(new ZipEntry("users.csv"));
            out.write("x".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("../outside.csv"));
            out.write("x".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        assertThrows(InvalidDataException.class, () -> service.restore(evil));
        assertFalse(Files.exists(temp.resolve("outside.csv")));
        assertEquals("id,username\n1,admin\n", Files.readString(data.resolve("users.csv")));
    }

    @Test
    void aZipWithoutTheUsersFileIsNotABackup() throws IOException {
        Path other = temp.resolve("other.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(other))) {
            out.putNextEntry(new ZipEntry("members.csv"));
            out.write("x".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        assertThrows(InvalidDataException.class, () -> service.restore(other));
    }

    @Test
    void aFileThatIsNotAZipOrDoesNotExistIsRefused() throws IOException {
        Path text = temp.resolve("plain.zip");
        Files.writeString(text, "hello", StandardCharsets.UTF_8);

        assertThrows(InvalidDataException.class, () -> service.restore(text));
        assertThrows(InvalidDataException.class, () -> service.restore(temp.resolve("missing.zip")));
    }

    @Test
    void theDailyBackupIsMadeOnlyOnceAPerDay() {
        assertNotNull(service.backupIfNoneToday(5));
        assertNull(service.backupIfNoneToday(5));
        assertEquals(1, service.list().size());
    }

    @Test
    void theOldestAutomaticBackupsAreRemoved() throws IOException {
        Files.createDirectories(backups);
        for (int day = 1; day <= 4; day++) {
            Path old = backups.resolve("nextfit-respaldo-2026110" + day + "-100000.zip");
            Files.write(old, new byte[] {0});
            Files.setLastModifiedTime(old, java.nio.file.attribute.FileTime.from(
                    Instant.parse("2026-11-0" + day + "T10:00:00Z")));
        }

        service.backupIfNoneToday(3);

        List<String> kept = service.list().stream().map(BackupInfo::name).toList();
        assertEquals(3, kept.size());
        assertFalse(kept.contains("nextfit-respaldo-20261101-100000.zip"));
        assertFalse(kept.contains("nextfit-respaldo-20261102-100000.zip"));
    }

    @Test
    void theDateOfABackupComesFromItsNameNotFromTheFileTime() throws IOException {
        Path first = service.backupIfNoneToday(5);
     Files.setLastModifiedTime(first, java.nio.file.attribute.FileTime.from(Instant.parse("2030-01-01T00:00:00Z")));

        assertNull(service.backupIfNoneToday(5));
     assertEquals(java.time.LocalDate.of(2026, 11, 8), service.list().get(0).createdAt().toLocalDate());
    }
}
