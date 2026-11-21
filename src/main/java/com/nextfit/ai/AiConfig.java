package com.nextfit.ai;

import com.nextfit.exceptions.AiException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Settings of the AI assistant, read from the same local file as the database
 * ({@code nextfit.properties}), which is never committed. The key is paid by use, so it must
 * never be written in the code or sent to GitHub. When the file or the key does not exist the
 * assistant is simply turned off and the rest of NextFit works as always.
 *
 * @param apiKey      the key of the AI service
 * @param model       the model that answers
 * @param dailyTokens the tokens that may be used each day
 */
public record AiConfig(String apiKey, String model, int dailyTokens) {

    /** Model used when the file does not say another one. */
    public static final String DEFAULT_MODEL = "claude-sonnet-5-5";

    private static final String KEY_LINE = "ai.apiKey";
    private static final String MODEL_LINE = "ai.model";
    private static final String TOKENS_LINE = "ai.dailyTokens";

    /**
     * Reads the settings of the assistant.
     *
     * @param file the settings file
     * @return the settings, or empty when there is no file or no key, which means "assistant off"
     * @throws AiException when the file cannot be read or a number is wrong
     */
    public static Optional<AiConfig> load(Path file) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new AiException("No se pudo leer el archivo de configuración", e);
        }
        String key = properties.getProperty(KEY_LINE);
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String model = properties.getProperty(MODEL_LINE);
        return Optional.of(new AiConfig(
                key.trim(),
                model == null || model.isBlank() ? DEFAULT_MODEL : model.trim(),
                tokens(properties.getProperty(TOKENS_LINE))));
    }

    private static int tokens(String text) {
        if (text == null || text.isBlank()) {
            return DailyTokenBudget.DEFAULT_MAX_TOKENS;
        }
        try {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) {
                throw new AiException("La línea " + TOKENS_LINE + " debe ser mayor que cero");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new AiException("La línea " + TOKENS_LINE + " debe ser un número");
        }
    }

    /** Hides the key so it never appears in logs. */
    @Override
    public String toString() {
        return "AiConfig[apiKey=***, model=" + model + ", dailyTokens=" + dailyTokens + "]";
    }
}