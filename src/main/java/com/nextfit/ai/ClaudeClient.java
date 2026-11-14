package com.nextfit.ai;

import com.nextfit.exceptions.AiException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The real AI service: talks to the Claude API over HTTPS using only what Java already has. The
 * assistant does not know this class, only {@link AiClient}. The key travels in a header and is
 * never written in an error message, in a log or in the code.
 */
public class ClaudeClient implements AiClient {

    /** Address of the Claude messages service. */
    public static final URI DEFAULT_ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");

    private static final String API_VERSION = "2023-06-01";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(60);

    private final AiConfig config;
    private final URI endpoint;
    private final HttpClient http;

    /**
     * Creates the client for the real service.
     *
     * @param config the key and the model
     */
    public ClaudeClient(AiConfig config) {
        this(config, DEFAULT_ENDPOINT);
    }

    /**
     * Creates the client for another address, so the tests can use a local fake server.
     *
     * @param config   the key and the model
     * @param endpoint the address of the service
     */
    public ClaudeClient(AiConfig config, URI endpoint) {
        this.config = config;
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public AiReply send(AiRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(ANSWER_TIMEOUT)
                .header("content-type", "application/json")
                .header("anthropic-version", API_VERSION)
                .header("x-api-key", config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(bodyOf(request), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AiException("No se pudo conectar con el asistente. Revisa tu conexión a internet", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Se interrumpió la consulta al asistente", e);
        }
        if (response.statusCode() != 200) {
            throw new AiException(messageForStatus(response.statusCode()));
        }
        return replyOf(response.body());
    }

    // EXPLICACIÓN: el mensaje depende solo del código; nunca se copia el cuerpo, para no mostrar datos de más
    private static String messageForStatus(int status) {
        return switch (status) {
            case 401, 403 -> "La clave del asistente no es válida o no tiene permiso";
            case 429 -> "El asistente está recibiendo demasiadas consultas. Intenta en un momento";
            case 529 -> "El servicio del asistente está saturado. Intenta en un momento";
            default -> "El asistente no pudo responder (código " + status + ")";
        };
    }

    private String bodyOf(AiRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.model());
        body.put("max_tokens", request.maxTokens());
        body.put("system", request.system());
        List<Object> messages = new ArrayList<>();
        for (AiMessage message : request.messages()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", message.role());
            entry.put("content", message.blocks().stream().map(ClaudeClient::blockToJson).toList());
            messages.add(entry);
        }
        body.put("messages", messages);
        if (!request.tools().isEmpty()) {
            body.put("tools", request.tools().stream().map(ClaudeClient::toolToJson).toList());
        }
        return Json.write(body);
    }

    private static Map<String, Object> blockToJson(AiBlock block) {
        Map<String, Object> json = new LinkedHashMap<>();
        switch (block) {
            case AiBlock.Text text -> {
                json.put("type", "text");
                json.put("text", text.text());
            }
            case AiBlock.ToolUse use -> {
                json.put("type", "tool_use");
                json.put("id", use.id());
                json.put("name", use.name());
                json.put("input", use.input());
            }
            case AiBlock.ToolResult result -> {
                json.put("type", "tool_result");
                json.put("tool_use_id", result.toolUseId());
                json.put("content", result.content());
                if (result.error()) {
                    json.put("is_error", true);
                }
            }
        }
        return json;
    }

    private static Map<String, Object> toolToJson(AiTool tool) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", tool.name());
        json.put("description", tool.description());
        json.put("input_schema", tool.inputSchema());
        return json;
    }

    private static AiReply replyOf(String body) {
        try {
            Map<?, ?> root = (Map<?, ?>) Json.parse(body);
            List<AiBlock> blocks = new ArrayList<>();
            for (Object item : (List<?>) root.get("content")) {
                Map<?, ?> block = (Map<?, ?>) item;
                String type = (String) block.get("type");
                if ("text".equals(type)) {
                    blocks.add(new AiBlock.Text((String) block.get("text")));
                } else if ("tool_use".equals(type)) {
                    blocks.add(new AiBlock.ToolUse(
                            (String) block.get("id"), (String) block.get("name"), inputOf(block.get("input"))));
                }
            }
            Map<?, ?> usage = (Map<?, ?>) root.get("usage");
            return new AiReply(blocks, (String) root.get("stop_reason"),
                    ((Number) usage.get("input_tokens")).intValue(),
                    ((Number) usage.get("output_tokens")).intValue());
        } catch (ClassCastException | NullPointerException e) {
            throw new AiException("El asistente respondió algo que no se entiende", e);
        }
    }

    private static Map<String, Object> inputOf(Object input) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (input instanceof Map<?, ?> map) {
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
        }
        return result;
    }
}