package com.nextfit.util;

import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotoStorageTest {

    private Path workFolder;
    private PhotoStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        workFolder = Files.createTempDirectory("nextfit-photos");
        storage = new PhotoStorage(workFolder.resolve("photos"));
    }

    private Path fileNamed(String name, int bytes) throws IOException {
        return Files.write(workFolder.resolve(name), new byte[bytes]);
    }

    @Test
    void copyStoresThePhotoWithTheDocumentNumberAsName() throws IOException {
        String stored = storage.copy(fileNamed("selfie.PNG", 10), "1065123456");

        assertTrue(stored.endsWith("photos/1065123456.png"));
        assertTrue(Files.exists(Path.of(stored)));
    }

    @Test
    void copyReplacesThePreviousPhotoOfTheSameMember() throws IOException {
        storage.copy(fileNamed("a.jpg", 10), "1065123456");
        String stored = storage.copy(fileNamed("b.jpg", 20), "1065123456");

        assertEquals(20L, Files.size(Path.of(stored)));
    }

    @Test
    void copyRejectsFilesThatAreNotImages() throws IOException {
        Path notAnImage = fileNamed("notes.txt", 10);

        assertThrows(InvalidDataException.class, () -> storage.copy(notAnImage, "1065123456"));
    }

    @Test
    void copyRejectsPhotosOverFiveMegabytes() throws IOException {
        Path huge = fileNamed("huge.png", 5 * 1024 * 1024 + 1);

        assertThrows(InvalidDataException.class, () -> storage.copy(huge, "1065123456"));
    }

    @Test
    void copyCleansTheDocumentNumberBeforeUsingItAsAFileName() throws IOException {
        String stored = storage.copy(fileNamed("p.png", 10), "../evil");

        assertTrue(stored.endsWith("photos/___evil.png"));
    }
}