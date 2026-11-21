package com.nextfit.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the CSV file format with real files in a temporary folder. */
class CsvStoreTest {

    @TempDir
    Path folder;

    private Path file;

    @BeforeEach
    void setUp() {
        file = folder.resolve("data.csv");
    }

    @Test
    void aNewStoreCreatesTheFileWithOnlyItsHeader() throws IOException {
        new CsvStore(file, "id", "name");

        assertEquals("id,name\n", Files.readString(file, StandardCharsets.UTF_8));
        assertTrue(new CsvStore(file, "id", "name").readAll().isEmpty());
    }

    @Test
    void openingAnExistingStoreKeepsItsRows() {
        CsvStore first = new CsvStore(file, "id", "name");
        first.writeAll(List.<String[]>of(new String[]{"1", "Ana"}));

        List<String[]> rows = new CsvStore(file, "id", "name").readAll();

        assertEquals(1, rows.size());
        assertArrayEquals(new String[]{"1", "Ana"}, rows.get(0));
    }

    @Test
    void commasQuotesAndNewlinesSurviveTheTrip() {
        CsvStore store = new CsvStore(file, "id", "text");
        String[] hostile = {"1", "Pérez, Ana \"la jefa\"\nsegunda línea\r\ntercera"};
        store.writeAll(List.<String[]>of(hostile));

        List<String[]> rows = store.readAll();

        assertEquals(1, rows.size());
        assertArrayEquals(hostile, rows.get(0));
    }

    @Test
    void accentsAndEmojiAreKeptAsUtf8() {
        CsvStore store = new CsvStore(file, "name");
        store.writeAll(List.<String[]>of(new String[]{"Ñandú Ángel 💪"}));

        assertEquals("Ñandú Ángel 💪", store.readAll().get(0)[0]);
    }

    @Test
    void emptyAndNullFieldsAreKeptInTheirPlace() {
        CsvStore store = new CsvStore(file, "a", "b", "c");
        store.writeAll(List.<String[]>of(new String[]{"", "x", null}));

        String[] row = store.readAll().get(0);

        assertEquals(3, row.length);
        assertEquals("", row[0]);
        assertEquals("x", row[1]);
        assertEquals("", row[2], "a null field is stored as an empty one");
    }

    @Test
    void windowsLineEndingsAndBlankLinesAreTolerated() throws IOException {
        Files.writeString(file, "id,name\r\n1,Ana\r\n\r\n2,Luis\r\n", StandardCharsets.UTF_8);

        List<String[]> rows = new CsvStore(file, "id", "name").readAll();

        assertEquals(2, rows.size());
        assertEquals("Luis", rows.get(1)[1]);
    }

    @Test
    void aFileWithoutTheFinalNewlineKeepsItsLastRow() throws IOException {
        Files.writeString(file, "id,name\n1,Ana", StandardCharsets.UTF_8);

        List<String[]> rows = new CsvStore(file, "id", "name").readAll();

        assertEquals(1, rows.size());
        assertEquals("Ana", rows.get(0)[1]);
    }

    @Test
    void writingReplacesEverythingAndLeavesNoTemporaryFile() throws IOException {
        CsvStore store = new CsvStore(file, "id");
        store.writeAll(List.of(new String[]{"1"}, new String[]{"2"}, new String[]{"3"}));
        store.writeAll(List.<String[]>of(new String[]{"9"}));

        assertEquals(1, store.readAll().size());
        assertEquals("9", store.readAll().get(0)[0]);
        try (var files = Files.list(folder)) {
            assertEquals(List.of("data.csv"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void theFolderIsCreatedWhenItDoesNotExist() {
        Path nested = folder.resolve("a").resolve("b").resolve("data.csv");

        new CsvStore(nested, "id");

        assertTrue(Files.isRegularFile(nested));
    }

    @Test
    void manyRowsKeepTheirOrder() {
        CsvStore store = new CsvStore(file, "n");
        List<String[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 500; i++) {
            rows.add(new String[]{String.valueOf(i)});
        }
        store.writeAll(rows);

        List<String[]> read = store.readAll();

        assertEquals(500, read.size());
        assertEquals("0", read.get(0)[0]);
        assertEquals("499", read.get(499)[0]);
        assertFalse(read.stream().anyMatch(r -> r.length != 1));
    }
}
