package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAssistantCompleteTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private final List<AiRequest> requests = new ArrayList<>();

    private static AiReply reply(String text, String stopReason, int in, int out) {
        return new AiReply(List.of(new AiBlock.Text(text)), stopReason, in, out);
    }

    private AiAssistant assistant(AiClient client, int dailyTokens) {
        AiClient recording = request -> {
            requests.add(request);
            return client.send(request);
        };
        return new AiAssistant(recording, List.of(), new DailyTokenBudget(dailyTokens, CLOCK), CLOCK);
    }

    @Test
    void returnsTheTextAndSendsOneRequestWithoutTools() {
        AiAssistant assistant = assistant(request -> reply("{\"ok\":true}", AiReply.END_TURN, 10, 5), 1000);

        String text = assistant.complete("Eres un entrenador", "Arma una rutina", 800);

        assertEquals("{\"ok\":true}", text);
        assertEquals(1, requests.size());
        assertEquals("Eres un entrenador", requests.get(0).system());
        assertEquals(1, requests.get(0).messages().size());
        assertTrue(requests.get(0).tools().isEmpty());
        assertEquals(800, requests.get(0).maxTokens());
    }

    @Test
    void theTokensCountInTheDailyLimit() {
        AiAssistant assistant = assistant(request -> reply("hola", AiReply.END_TURN, 60, 40), 100);

        assistant.complete("s", "p", 100);

        AiException limit = assertThrowsAi(() -> assistant.complete("s", "p", 100));
        assertTrue(limit.getMessage().contains("límite de uso diario"));
        assertEquals(1, requests.size());
    }

    @Test
    void anEmptyAnswerIsAnError() {
        AiAssistant assistant = assistant(request -> reply("   ", AiReply.END_TURN, 1, 1), 1000);

        assertThrows(AiException.class, () -> assistant.complete("s", "p", 100));
    }

    @Test
    void anAnswerCutByTheLimitOfTokensIsAnError() {
        AiAssistant assistant = assistant(request -> reply("{\"days\":[", AiReply.MAX_TOKENS, 1, 100), 1000);

        assertThrows(AiException.class, () -> assistant.complete("s", "p", 100));
    }

    @Test
    void aFailureOfTheClientGoesUp() {
        AiAssistant assistant = assistant(request -> {
            throw new AiException("El asistente está saturado");
        }, 1000);

        AiException error = assertThrowsAi(() -> assistant.complete("s", "p", 100));

        assertEquals("El asistente está saturado", error.getMessage());
    }

    @Test
    void anEmptyPromptOrABadLimitIsRefusedBeforeCallingTheAi() {
        AiAssistant assistant = assistant(request -> reply("hola", AiReply.END_TURN, 1, 1), 1000);

        assertThrows(InvalidDataException.class, () -> assistant.complete("s", " ", 100));
        assertThrows(InvalidDataException.class, () -> assistant.complete("s", null, 100));
        assertThrows(InvalidDataException.class, () -> assistant.complete("s", "p", 0));
        assertThrows(InvalidDataException.class, () -> assistant.complete("s", "p", 100_000));
        assertTrue(requests.isEmpty());
    }

    private static AiException assertThrowsAi(Runnable action) {
        try {
            action.run();
        } catch (AiException e) {
            return e;
        }
        throw new AssertionError("expected an AiException");
    }
}