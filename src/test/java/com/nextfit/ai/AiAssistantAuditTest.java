package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import com.nextfit.model.AuditAction;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that the use of the assistant is audited without writing what was asked or answered. */
class AiAssistantAuditTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);
    private static final AiUser ADMIN = new AiUser(AiAudience.ADMIN, 0, "admin");
    private static final AiUser MEMBER = AiUser.member(7, "Ana");

    private final List<String> who = new ArrayList<>();
    private final List<AuditAction> actions = new ArrayList<>();
    private final List<String> details = new ArrayList<>();

    private void record(String username, AuditAction action, String detail) {
        who.add(username);
        actions.add(action);
        details.add(detail);
    }

    private static AiTool tool(String name) {
        return new AiTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return "test";
            }

            @Override
            public Map<String, Object> inputSchema() {
                return AiTool.noInput();
            }

            @Override
            public boolean allowedFor(AiAudience audience) {
                return true;
            }

            @Override
            public String run(Map<String, Object> input, AiUser user) {
                return "datos";
            }
        };
    }

    private AiAssistant assistant(AiClient client) {
        return new AiAssistant(client, List.of(tool("members_at_risk")),
                new DailyTokenBudget(100_000, CLOCK), CLOCK, this::record);
    }

    @Test
    void anAnsweredQuestionWritesWhoToolsAndTokens() {
        List<AiReply> replies = new ArrayList<>(List.of(
                new AiReply(List.of(new AiBlock.ToolUse("t1", "members_at_risk", Map.of())), AiReply.TOOL_USE, 100, 20),
                new AiReply(List.of(new AiBlock.Text("listo")), AiReply.END_TURN, 150, 30)));
        AiAssistant assistant = assistant(request -> replies.remove(0));

        assistant.ask(ADMIN, new AiConversation(), "¿Quién se va a ir?");

        assertEquals(List.of("admin"), who);
        assertEquals(List.of(AuditAction.AI_QUESTION), actions);
        assertEquals("Personal; herramientas: members_at_risk; tokens: 300", details.get(0));
    }

    @Test
    void theTextOfTheQuestionIsNeverWritten() {
        AiAssistant assistant = assistant(request ->
                new AiReply(List.of(new AiBlock.Text("respuesta")), AiReply.END_TURN, 10, 5));

        assistant.ask(MEMBER, new AiConversation(), "Mi clave secreta es 12345");

        assertEquals("Ana", who.get(0));
        assertTrue(details.get(0).startsWith("Socio; herramientas: ninguna"));
        assertFalse(details.get(0).contains("12345"));
        assertFalse(details.get(0).contains("respuesta"));
    }

    @Test
    void aFailedQuestionIsWrittenAsWithoutAnswer() {
        AiAssistant assistant = assistant(request -> {
            throw new AiException("sin conexión");
        });

        assertThrows(AiException.class, () -> assistant.ask(ADMIN, new AiConversation(), "Hola"));

        assertEquals(1, actions.size());
        assertTrue(details.get(0).endsWith("sin respuesta"));
    }

    @Test
    void aQuestionThatIsRejectedBeforeCallingTheAiIsNotWritten() {
        AiAssistant assistant = assistant(request -> {
            throw new AssertionError("no debe llamar a la IA");
        });

        assertThrows(RuntimeException.class, () -> assistant.ask(ADMIN, new AiConversation(), "  "));

        assertTrue(actions.isEmpty());
    }

    @Test
    void ifTheAuditFailsThePersonStillGetsTheAnswer() {
        AiAssistant assistant = new AiAssistant(
                request -> new AiReply(List.of(new AiBlock.Text("hola")), AiReply.END_TURN, 10, 5),
                List.of(), new DailyTokenBudget(100_000, CLOCK), CLOCK,
                (username, action, detail) -> {
                    throw new IllegalStateException("disco lleno");
                });

        assertEquals("hola", assistant.ask(ADMIN, new AiConversation(), "Hola"));
    }

    @Test
    void theSystemTextAsksForPlainText() {
        List<AiRequest> sent = new ArrayList<>();
        AiAssistant assistant = assistant(request -> {
            sent.add(request);
            return new AiReply(List.of(new AiBlock.Text("ok")), AiReply.END_TURN, 1, 1);
        });

        assistant.ask(ADMIN, new AiConversation(), "Hola");

        assertTrue(sent.get(0).system().contains("texto plano"));
        assertTrue(sent.get(0).system().contains("Markdown"));
    }
}