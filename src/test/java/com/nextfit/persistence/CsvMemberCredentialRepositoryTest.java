package com.nextfit.persistence;

import com.nextfit.model.MemberCredential;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvMemberCredentialRepositoryTest {

    @TempDir
    Path folder;

    @Test
    void startsEmptyAndFindsWhatWasSaved() {
        CsvMemberCredentialRepository repo = new CsvMemberCredentialRepository(folder.resolve("c.csv"));
        assertTrue(repo.find(3).isEmpty());

        repo.save(new MemberCredential(3, "PBKDF2$1000$abc$def"));

        MemberCredential read = new CsvMemberCredentialRepository(folder.resolve("c.csv")).find(3).orElseThrow();
        assertEquals("PBKDF2$1000$abc$def", read.passwordHash());
    }

    @Test
    void savingAgainReplacesAndDeleteOnlyRemovesThatMember() {
        CsvMemberCredentialRepository repo = new CsvMemberCredentialRepository(folder.resolve("c.csv"));
        repo.save(new MemberCredential(1, "old"));
        repo.save(new MemberCredential(2, "other"));
        repo.save(new MemberCredential(1, "new"));

        assertEquals("new", repo.find(1).orElseThrow().passwordHash());

        repo.delete(1);
        repo.delete(99);

        assertTrue(repo.find(1).isEmpty());
        assertEquals("other", repo.find(2).orElseThrow().passwordHash());
    }
}
