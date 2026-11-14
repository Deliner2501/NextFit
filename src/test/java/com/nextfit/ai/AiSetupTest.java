package com.nextfit.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSetupTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path folder;

    private Path write(String content) throws IOException {
        Path file = folder.resolve("nextfit.properties");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void withoutFileTheAssistantIsOff() {
        assertTrue(AiSetup.create(folder.resolve("nothing.properties"), List.of(), CLOCK).isEmpty());
    }

    @Test
    void withoutKeyTheAssistantIsOff() throws IOException {
        assertTrue(AiSetup.create(write("db.user=app\n"), List.of(), CLOCK).isEmpty());
    }

    @Test
    void withAKeyTheAssistantIsCreated() throws IOException {
        assertTrue(AiSetup.create(write("ai.apiKey=sk-test\n"), List.of(), CLOCK).isPresent());
    }

    @Test
    void wrongSettingsTurnTheAssistantOffInsteadOfStoppingNextFit() throws IOException {
        assertTrue(AiSetup.create(write("ai.apiKey=sk-test\nai.dailyTokens=mucho\n"), List.of(), CLOCK).isEmpty());
    }
}