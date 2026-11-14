package com.nextfit.ai;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiBlock;
import com.nextfit.ai.AiBlock;
import com.nextfit.ai.AiBlock;
import com.nextfit.ai.AiClient;
import com.nextfit.ai.AiClient;
import com.nextfit.ai.AiClient;
import com.nextfit.ai.AiConversation;
import com.nextfit.ai.AiConversation;
import com.nextfit.ai.AiConversation;
import com.nextfit.ai.AiMessage;
import com.nextfit.ai.AiMessage;
import com.nextfit.ai.AiMessage;
import com.nextfit.ai.AiReply;
import com.nextfit.ai.AiReply;
import com.nextfit.ai.AiReply;
import com.nextfit.ai.AiRequest;
import com.nextfit.ai.AiRequest;
import com.nextfit.ai.AiRequest;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.ai.AiUser;
import com.nextfit.ai.AiUser;
import com.nextfit.ai.DailyTokenBudget;
import com.nextfit.ai.DailyTokenBudget;
import com.nextfit.ai.DailyTokenBudget;
import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAssistantTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"), ZoneOffset.UTC);
    private static final AiUser ADMIN = new AiUser(AiAudience.ADMIN, 0, "admin");
    private static final AiUser TRAINER = new AiUser(AiAudience.TRAINER, 0, "carlos");
    private static final AiUser MEMBER = AiUser.member(7, "Ana");

    private FakeClient client;
    private DailyTokenBudget budget;
    private AiConversation chat;
    private FakeTool risk;
    private AiAssistant assistant;

    @BeforeEach
    void setUp() {
        client = new FakeClient();
        budget = new DailyTokenBudget(100_000, CLOCK);
        chat = new AiConversation();
        risk = new FakeTool("members_at_risk", AiAudience.ADMIN, AiAudience.RECEPTION);
        assistant = new AiAssistant(client, List.of(risk), budget, CLOCK);
    }

    private static AiReply text(String text) {
        return new AiReply(List.of(new AiBlock.Text(text)), AiReply.END_TURN, 100, 20);
    }

    private static AiReply toolUse(String id, String name) {
        return new AiReply(List.of(new AiBlock.ToolUse(id, name, Map.of())), AiReply.TOOL_USE, 100, 20);
    }

    // The result that the AI received in its second call, after asking for a tool.
    private AiBlock.ToolResult resultOfTheSecondCall() {
        return (AiBlock.ToolResult) client.requests.get(1).messages().get(2).blocks().get(0);
    }

    @Test
    void answersWithoutTools() {
        client.replies.add(text("Hola, ¿en qué te ayudo?"));

        String answer = assistant.ask(ADMIN, chat, "Hola");

        assertEquals("Hola, ¿en qué te ayudo?", answer);
        assertEquals(2, chat.size());
        assertEquals(0, risk.calls);
    }

    @Test
    void runsAToolAndGivesTheResultBack() {
        client.replies.add(toolUse("t1", "members_at_risk"));
        client.replies.add(text("Luis está en riesgo alto"));

        String answer = assistant.ask(ADMIN, chat, "¿Quién se va a ir?");

        assertEquals("Luis está en riesgo alto", answer);
        assertEquals(1, risk.calls);
        assertEquals(ADMIN, risk.lastUser);
        assertEquals(2, client.requests.size());
        assertEquals(3, client.requests.get(1).messages().size());
        AiBlock.ToolResult result = resultOfTheSecondCall();
        assertEquals("t1", result.toolUseId());
        assertEquals("Luis (80)", result.content());
        assertFalse(result.error());
        assertEquals(4, chat.size());
    }

    @Test
    void offersOnlyTheToolsAllowedForThePerson() {
        client.replies.add(text("ok"));
        client.replies.add(text("ok"));

        assistant.ask(TRAINER, chat, "hola");
        assistant.ask(ADMIN, new AiConversation(), "hola");

        assertTrue(client.requests.get(0).tools().isEmpty());
        assertEquals(1, client.requests.get(1).tools().size());
    }

    @Test
    void aToolThatIsNotAllowedIsNotRunEvenIfTheAiAsksForIt() {
        client.replies.add(toolUse("t1", "members_at_risk"));
        client.replies.add(text("No puedo ayudarte con eso"));

        assistant.ask(TRAINER, chat, "dime los socios en riesgo");

        assertEquals(0, risk.calls);
        assertTrue(resultOfTheSecondCall().error());
    }

    @Test
    void aBusinessErrorOfAToolGoesBackAsAnErrorResult() {
        risk.result = () -> {
            throw new BusinessRuleException("Socio no encontrado");
        };
        client.replies.add(toolUse("t1", "members_at_risk"));
        client.replies.add(text("No lo encontré"));

        assistant.ask(ADMIN, chat, "pregunta");

        assertTrue(resultOfTheSecondCall().error());
        assertEquals("Socio no encontrado", resultOfTheSecondCall().content());
    }

    @Test
    void anUnexpectedFailureIsNotShown() {
        risk.result = () -> {
            throw new IllegalStateException("clave secreta 123");
        };
        client.replies.add(toolUse("t1", "members_at_risk"));
        client.replies.add(text("Hubo un problema"));

        assistant.ask(ADMIN, chat, "pregunta");

        assertEquals("La herramienta falló", resultOfTheSecondCall().content());
    }

    @Test
    void longResultsAreCut() {
        risk.result = () -> "x".repeat(10_000);
        client.replies.add(toolUse("t1", "members_at_risk"));
        client.replies.add(text("listo"));

        assistant.ask(ADMIN, chat, "pregunta");

        String content = resultOfTheSecondCall().content();
        assertTrue(content.endsWith("(recortado)"));
        assertTrue(content.length() < AiAssistant.MAX_RESULT_CHARS + 30);
    }

    @Test
    void tooManyRoundsStopsAndLeavesTheChatAsItWas() {
        AiClient endless = request -> toolUse("t", "members_at_risk");
        assistant = new AiAssistant(endless, List.of(risk), budget, CLOCK);

        assertThrows(AiException.class, () -> assistant.ask(ADMIN, chat, "pregunta"));

        assertEquals(0, chat.size());
        assertEquals(AiAssistant.MAX_ROUNDS, risk.calls);
    }

    @Test
    void whenTheDailyLimitIsOverTheServiceIsNotCalled() {
        budget.record(60_000, 40_000);

        assertThrows(AiException.class, () -> assistant.ask(ADMIN, chat, "pregunta"));

        assertTrue(client.requests.isEmpty());
        assertEquals(0, chat.size());
    }

    @Test
    void theTokensOfEveryCallAreCounted() {
        client.replies.add(toolUse("t1", "members_at_risk"));
        client.replies.add(text("listo"));

        assistant.ask(ADMIN, chat, "pregunta");

        assertEquals(240, budget.usedToday());
    }

    @Test
    void emptyOrTooLongQuestionsAreRejected() {
        assertThrows(InvalidDataException.class, () -> assistant.ask(ADMIN, chat, "   "));
        assertThrows(InvalidDataException.class, () -> assistant.ask(ADMIN, chat, null));
        assertThrows(InvalidDataException.class,
                () -> assistant.ask(ADMIN, chat, "x".repeat(AiAssistant.MAX_QUESTION_CHARS + 1)));

        assertEquals(0, chat.size());
        assertTrue(client.requests.isEmpty());
    }

    @Test
    void aVeryLongChatMustBeStartedAgain() {
        for (int i = 0; i < AiAssistant.MAX_MESSAGES; i++) {
            chat.add(AiMessage.user("x"));
        }

        assertThrows(BusinessRuleException.class, () -> assistant.ask(ADMIN, chat, "otra pregunta"));
    }

    @Test
    void anAnswerWithoutTextHasAFallback() {
        client.replies.add(new AiReply(List.of(), AiReply.END_TURN, 10, 0));

        assertEquals("No tengo una respuesta para eso.", assistant.ask(ADMIN, chat, "pregunta"));
    }

    @Test
    void theInstructionsKnowTodayAndWhoIsAsking() {
        client.replies.add(text("ok"));
        client.replies.add(text("ok"));

        assistant.ask(ADMIN, chat, "hola");
        assistant.ask(MEMBER, new AiConversation(), "¿qué me toca hoy?");

        String staff = client.requests.get(0).system();
        assertTrue(staff.contains("13 de noviembre de 2026"));
        assertTrue(staff.contains("admin"));
        String member = client.requests.get(1).system();
        assertTrue(member.contains("solo puedes hablar de sus propios datos"));
        assertFalse(member.contains("Ana"));
    }

    /** An AI service that answers what the test prepared and remembers what it was asked. */
    private static final class FakeClient implements AiClient {

        private final Deque<AiReply> replies = new ArrayDeque<>();
        private final List<AiRequest> requests = new ArrayList<>();

        @Override
        public AiReply send(AiRequest request) {
            requests.add(request);
            AiReply reply = replies.poll();
            if (reply == null) {
                throw new AiException("El cliente falso no tiene más respuestas");
            }
            return reply;
        }
    }

    /** A tool that counts how many times it was run and who asked. */
    private static final class FakeTool implements AiTool {

        private final String name;
        private final Set<AiAudience> audiences;
        private Supplier<String> result = () -> "Luis (80)";
        private int calls;
        private AiUser lastUser;

        FakeTool(String name, AiAudience... audiences) {
            this.name = name;
            this.audiences = Set.of(audiences);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "Herramienta de prueba";
        }

        @Override
        public Map<String, Object> inputSchema() {
            return AiTool.noInput();
        }

        @Override
        public boolean allowedFor(AiAudience audience) {
            return audiences.contains(audience);
        }

        @Override
        public String run(Map<String, Object> input, AiUser user) {
            calls++;
            lastUser = user;
            return result.get();
        }
    }
}