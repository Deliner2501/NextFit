package com.nextfit.persistence;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessKeyStoreTest {

    @Test
    void createsTheKeyOnceAndReadsTheSameOneAfterwards() throws Exception {
        Path dir = Files.createTempDirectory("nextfit-key");
        Path file = dir.resolve("sub").resolve("access.key");

        byte[] first = AccessKeyStore.loadOrCreate(file);
        byte[] second = AccessKeyStore.loadOrCreate(file);

        assertEquals(32, first.length);
        assertTrue(Arrays.equals(first, second));
        assertTrue(Files.exists(file));
    }

    @Test
    void aTooShortFileIsReplacedByAValidKey() throws Exception {
        Path file = Files.createTempFile("nextfit-key", ".key");
        Files.write(file, new byte[] {1, 2, 3});

        assertEquals(32, AccessKeyStore.loadOrCreate(file).length);
    }
}
