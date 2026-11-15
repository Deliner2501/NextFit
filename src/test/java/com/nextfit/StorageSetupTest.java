package com.nextfit;

import com.nextfit.persistence.CsvStorage;
import com.nextfit.persistence.Storage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StorageSetupTest {

    @TempDir
    Path dir;

    @Test
    void usesTheCsvFilesWhenThereIsNoSettingsFile() {
        Storage storage = StorageSetup.create(dir, dir.resolve("nextfit.properties"));

        assertInstanceOf(CsvStorage.class, storage);
    }

    @Test
    void usesTheCsvFilesWhenTheDatabaseIsNotEnabled() throws IOException {
        Path settings = write("db.enabled=false\ndb.url=jdbc:nada:invalid\ndb.user=u\ndb.password=p\n");

        assertInstanceOf(CsvStorage.class, StorageSetup.create(dir, settings));
    }

    @Test
    void usesTheCsvFilesWhenTheDatabaseIsEnabledButTheSettingsAreIncomplete() throws IOException {
        Path settings = write("db.enabled=true\n");

        assertInstanceOf(CsvStorage.class, StorageSetup.create(dir, settings));
    }

    @Test
    void usesTheCsvFilesWhenTheDatabaseCannotBeReached() throws IOException {
        Path settings = write("db.enabled=true\ndb.url=jdbc:nada:invalid\ndb.user=u\ndb.password=p\n");

        assertInstanceOf(CsvStorage.class, StorageSetup.create(dir, settings));
    }

    private Path write(String text) throws IOException {
        return Files.writeString(dir.resolve("nextfit.properties"), text);
    }
}
