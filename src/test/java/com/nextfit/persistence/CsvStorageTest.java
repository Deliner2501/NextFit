package com.nextfit.persistence;

import com.nextfit.StorageSetup;
import com.nextfit.model.MemberCredential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvStorageTest {

    @TempDir
    Path dir;

    @Test
    void memberPasswordsAreKeptInTheCsvFileAndSurviveARestart() {
        new CsvStorage(dir).memberCredentials().save(new MemberCredential(7, "PBKDF2$1000$salt$hash"));

        MemberCredential read = new CsvStorage(dir).memberCredentials().find(7).orElseThrow();

        assertEquals("PBKDF2$1000$salt$hash", read.passwordHash());
        assertTrue(Files.exists(dir.resolve("member_credentials.csv")));
    }

    @Test
    void whenOracleIsNotUsedTheMemberPasswordsStillWorkOnTheCsvBackup() {
        Storage storage = StorageSetup.create(dir, dir.resolve("nextfit.properties"));

        storage.memberCredentials().save(new MemberCredential(3, "first"));
        storage.memberCredentials().save(new MemberCredential(3, "second"));

        assertEquals("second", storage.memberCredentials().find(3).orElseThrow().passwordHash());
        storage.memberCredentials().delete(3);
        assertTrue(storage.memberCredentials().find(3).isEmpty());
    }
}
