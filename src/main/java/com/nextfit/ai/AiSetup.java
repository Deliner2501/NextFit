package com.nextfit.ai;

import com.nextfit.exceptions.AiException;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Builds the assistant from the local settings file. It is the only place that knows how the
 * pieces are joined (service, limit, tools), so {@code Main} and the screens do not. Without a key
 * the result is empty and the assistant is simply off: NextFit keeps working as always.
 */
public final class AiSetup {

    private AiSetup() {
    }

    /**
     * Creates the assistant if the settings file has a key.
     *
     * @param settingsFile the local file that holds the key
     * @param tools        all the tools of the assistant
     * @param clock        the source of today's date
     * @return the assistant, or empty when it is off or the settings are wrong
     */
    public static Optional<AiAssistant> create(Path settingsFile, List<AiTool> tools, Clock clock) {
        try {
            return AiConfig.load(settingsFile).map(config -> new AiAssistant(
                    new ClaudeClient(config), tools, new DailyTokenBudget(config.dailyTokens(), clock), clock));
        } catch (AiException e) {
            // EXPLICACIÓN: una configuración mala no debe impedir que NextFit abra; el asistente queda apagado
            System.err.println("Asistente apagado: " + e.getMessage());
            return Optional.empty();
        }
    }
}