package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeClientTest {

    private static final String KEY = "sk-secreta-123";

    private HttpServer server;
    private int status;
    private String answer;
    private String receivedBody;
    private String receivedKey;
    private String receivedVersion;
    private ClaudeClient client;

    @BeforeEach
    void setUp() throws IOException {
        status = 200;
        answer = "{\"content\":[{\"type\":\"text\",\"text\":\"Hola\"}],\"stop_reason\":\"end_turn\","
                + "\"usage\":{\"input_tokens\":12,\"output_tokens\":3}}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/messages", exchange -> {
            receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            receivedKey = exchange.getRequestHeaders().getFirst("x-api-key");
            receivedVersion = exchange.getRequestHeaders().getFirst("anthropic-version");
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        client = new ClaudeClient(new AiConfig(KEY, "claude-sonnet-5-5", 1000), uri());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages");
    }

    private static AiRequest request(List<AiMessage> messages, List<AiTool> tools) {
        return new AiRequest("Eres un asistente", messages, tools, 500);
    }

    @Test
    void sendsTheKeyTheVersionAndTheRequest() {
        client.send(request(List.of(AiMessage.user("Hola")), List.of()));

        assertEquals(KEY, receivedKey);
        assertEquals("2023-06-01", receivedVersion);
        Map<?, ?> body = (Map<?, ?>) Json.parse(receivedBody);
        assertEquals("claude-sonnet-5-5", body.get("model"));
        assertEquals(500L, body.get("max_tokens"));
        assertEquals("Eres un asistente", body.get("system"));
        assertFalse(body.containsKey("tools"));
        Map<?, ?> message = (Map<?, ?>) ((List<?>) body.get("messages")).get(0);
        assertEquals("user", message.get("role"));
    }

    @Test
    void readsTheTextAndTheTokens() {
        AiReply reply = client.send(request(List.of(AiMessage.user("Hola")), List.of()));

        assertEquals("Hola", reply.text());
        assertEquals(AiReply.END_TURN, reply.stopReason());
        assertEquals(12, reply.inputTokens());
        assertEquals(3, reply.outputTokens());
    }

    @Test
    void readsARequestForATool() {
        answer = "{\"content\":[{\"type\":\"text\",\"text\":\"Reviso\"},"
                + "{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"members_at_risk\",\"input\":{\"limit\":5}}],"
                + "\"stop_reason\":\"tool_use\",\"usage\":{\"input_tokens\":20,\"output_tokens\":8}}";

        AiReply reply = client.send(request(List.of(AiMessage.user("Hola")), List.of()));

        assertEquals(1, reply.toolUses().size());
        assertEquals("toolu_1", reply.toolUses().get(0).id());
        assertEquals("members_at_risk", reply.toolUses().get(0).name());
        assertEquals(5L, reply.toolUses().get(0).input().get("limit"));
        assertEquals(AiReply.TOOL_USE, reply.stopReason());
    }

    @Test
    void sendsTheToolsAndTheirResults() {
        AiTool tool = new AiTool() {
            public String name() { return "members_at_risk"; }
            public String description() { return "Socios en riesgo"; }
            public Map<String, Object> inputSchema() { return AiTool.noInput(); }
            public boolean allowedFor(AiAudience audience) { return true; }
            public String run(Map<String, Object> input, AiUser user) { return ""; }
        };
        List<AiMessage> messages = List.of(
                AiMessage.user("Hola"),
                AiMessage.assistant(List.of(new AiBlock.ToolUse("toolu_1", "members_at_risk", Map.of()))),
                AiMessage.toolResults(List.of(new AiBlock.ToolResult("toolu_1", "falló", true))));

        client.send(request(messages, List.of(tool)));

        Map<?, ?> body = (Map<?, ?>) Json.parse(receivedBody);
        Map<?, ?> sentTool = (Map<?, ?>) ((List<?>) body.get("tools")).get(0);
        assertEquals("members_at_risk", sentTool.get("name"));
        assertTrue(sentTool.containsKey("input_schema"));
        Map<?, ?> resultMessage = (Map<?, ?>) ((List<?>) body.get("messages")).get(2);
        Map<?, ?> result = (Map<?, ?>) ((List<?>) resultMessage.get("content")).get(0);
        assertEquals("tool_result", result.get("type"));
        assertEquals("toolu_1", result.get("tool_use_id"));
        assertEquals(Boolean.TRUE, result.get("is_error"));
    }

    @Test
    void aRejectedKeyGivesAClearMessageWithoutShowingTheKey() {
        status = 401;
        answer = "{\"error\":{\"message\":\"invalid x-api-key " + KEY + "\"}}";

        AiException e = assertThrows(AiException.class,
                () -> client.send(request(List.of(AiMessage.user("Hola")), List.of())));

        assertTrue(e.getMessage().contains("clave"));
        assertFalse(e.getMessage().contains(KEY));
    }

    @Test
    void tooManyRequestsHaveTheirOwnMessage() {
        status = 429;

        AiException e = assertThrows(AiException.class,
                () -> client.send(request(List.of(AiMessage.user("Hola")), List.of())));

        assertTrue(e.getMessage().contains("demasiadas"));
    }

    @Test
    void anAnswerThatIsNotUnderstoodIsAnAiError() {
        answer = "{\"algo\":\"raro\"}";

        assertThrows(AiException.class,
                () -> client.send(request(List.of(AiMessage.user("Hola")), List.of())));
    }

    @Test
    void withoutConnectionTheMessageSaysSo() {
        server.stop(0);

        AiException e = assertThrows(AiException.class,
                () -> client.send(request(List.of(AiMessage.user("Hola")), List.of())));

        assertTrue(e.getMessage().contains("conexión"));
    }
}