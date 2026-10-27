package com.nextfit;

import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.persistence.CsvPaymentRepository;
import com.nextfit.persistence.CsvUserRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDataTest {

    @Test
    void anEmptyInstallationReceivesTheExampleData() throws IOException {
        Path dir = Files.createTempDirectory("nextfit-demo");

        assertTrue(DemoData.seedIfEmpty(dir));

        assertEquals(16, new CsvMemberRepository(dir.resolve("members.csv")).findAll().size());
        assertFalse(new CsvPaymentRepository(dir.resolve("payments.csv")).findAll().isEmpty());
        assertTrue(new CsvUserRepository(dir.resolve("users.csv")).findByUsername("admin").isPresent());
        assertTrue(new CsvUserRepository(dir.resolve("users.csv")).findByUsername(DemoData.RECEPTION_USER).isPresent());
    }

    @Test
    void existingDataIsNeverMixedWithTheExample() throws IOException {
        Path dir = Files.createTempDirectory("nextfit-demo");
        DemoData.seedIfEmpty(dir);
        int members = new CsvMemberRepository(dir.resolve("members.csv")).findAll().size();

        assertFalse(DemoData.seedIfEmpty(dir));

        assertEquals(members, new CsvMemberRepository(dir.resolve("members.csv")).findAll().size());
    }

    @Test
    void theExampleHistoryHasActiveExpiredAndInactiveMembers() throws IOException {
        Path dir = Files.createTempDirectory("nextfit-demo");
        DemoData.seedIfEmpty(dir);

        long inactive = new CsvMemberRepository(dir.resolve("members.csv")).findAll().stream()
                .filter(m -> !m.isActive()).count();
        assertEquals(1, inactive);
    }
}
