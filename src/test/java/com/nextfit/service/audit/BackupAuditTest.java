package com.nextfit.service.audit;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.service.BackupService;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupAuditTest {

    @TempDir
    Path temp;

    private List<String> events;
    private BackupService service;

    @BeforeEach
    void setUp() throws IOException {
        Path data = temp.resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve("users.csv"), "id,username\n1,admin\n", StandardCharsets.UTF_8);
        events = new ArrayList<>();
        AuditLog audit = (username, action, detail) -> events.add(username + "|" + action + "|" + detail);
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), ZoneOffset.UTC);
        service = new BackupService(data, temp.resolve("backups"), clock, audit, () -> "admin");
    }

    @Test
    void creatingABackupWritesWhoAndTheFileName() {
        Path file = service.createBackup();

        assertEquals(List.of("admin|BACKUP_CREATED|" + file.getFileName()), events);
    }

    @Test
    void restoringABackupWritesTheFileAndTheSafetyCopy() {
        Path file = service.createBackup();
        events.clear();

        Path safety = service.restore(file);

        assertEquals(1, events.size());
        assertTrue(events.get(0).startsWith("admin|BACKUP_RESTORED|" + file.getFileName()));
        assertTrue(events.get(0).contains(safety.getFileName().toString()));
    }

    @Test
    void aRefusedRestoreWritesNothing() throws IOException {
        Path notABackup = temp.resolve("plain.zip");
        Files.writeString(notABackup, "hello", StandardCharsets.UTF_8);

        assertThrows(InvalidDataException.class, () -> service.restore(notABackup));

        assertTrue(events.isEmpty());
    }
}