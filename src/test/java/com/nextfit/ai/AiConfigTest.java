package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigTest {

    @TempDir
    Path folder;

    private Path write(String content) throws IOException {
        Path file = folder.resolve("nextfit.properties");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void withoutFileTheAssistantIsOff() {
        assertTrue(AiConfig.load(folder.resolve("nothing.properties")).isEmpty());
    }

    @Test
    void withoutKeyTheAssistantIsOff() throws IOException {
        Optional<AiConfig> config = AiConfig.load(write("db.user=app\nai.apiKey=   \n"));

        assertTrue(config.isEmpty());
    }

    @Test
    void readsTheKeyAndUsesTheDefaults() throws IOException {
        AiConfig config = AiConfig.load(write("db.user=app\nai.apiKey= sk-test \n")).orElseThrow();

        assertEquals("sk-test", config.apiKey());
        assertEquals(AiConfig.DEFAULT_MODEL, config.model());
        assertEquals(DailyTokenBudget.DEFAULT_MAX_TOKENS, config.dailyTokens());
    }

    @Test
    void readsTheModelAndTheLimit() throws IOException {
        AiConfig config = AiConfig.load(
                write("ai.apiKey=sk-test\nai.model=claude-haiku-5-5\nai.dailyTokens=50000\n")).orElseThrow();

        assertEquals("claude-haiku-5-5", config.model());
        assertEquals(50_000, config.dailyTokens());
    }

    @Test
    void aWrongLimitIsRejected() throws IOException {
        assertThrows(AiException.class, () -> AiConfig.load(write("ai.apiKey=k\nai.dailyTokens=mucho\n")));
        assertThrows(AiException.class, () -> AiConfig.load(write("ai.apiKey=k\nai.dailyTokens=0\n")));
    }

    @Test
    void theKeyIsNeverShownWhenPrinted() throws IOException {
        AiConfig config = AiConfig.load(write("ai.apiKey=sk-secreta\n")).orElseThrow();

        assertFalse(config.toString().contains("sk-secreta"));
    }
}