package com.nextfit.web;

import com.nextfit.ai.Json;
import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalChatEndpointTest {

    private static final String JSON = "application/json";

    private final List<String> tokens = new ArrayList<>();
    private final List<String> questions = new ArrayList<>();

    private PortalChatEndpoint endpointThatAnswers() {
        return new PortalChatEndpoint((token, question) -> {
            tokens.add(token);
            questions.add(question);
            return "respuesta a " + question;
        });
    }

    private static PortalChatEndpoint endpointThatFails(RuntimeException failure) {
        BiFunction<String, String, String> asker = (token, question) -> {
            throw failure;
        };
        return new PortalChatEndpoint(asker);
    }

    private static Map<?, ?> read(PortalChatEndpoint.Response response) {
        return (Map<?, ?>) Json.parse(response.json());
    }

    @Test
    void answersAQuestionWithAJson() {
        PortalChatEndpoint.Response response = endpointThatAnswers().post("ANA", JSON, "{\"question\":\"hola\"}");

        assertEquals(200, response.status());
        assertEquals("respuesta a hola", read(response).get("answer"));
    }

    @Test
    void theMemberIsIdentifiedByTheCodeOfTheAddressAndNeverByTheBody() {
        endpointThatAnswers().post("ANA", JSON,
                "{\"question\":\"hola\",\"token\":\"OTRO\",\"memberId\":8,\"name\":\"Luis\"}");

        assertEquals(List.of("ANA"), tokens);
    }

    @Test
    void acceptsTheContentTypeWithACharset() {
        assertEquals(200, endpointThatAnswers().post("ANA", "application/json; charset=utf-8",
                "{\"question\":\"hola\"}").status());
    }

    @Test
    void aRequestThatIsNotJsonIsRefusedBeforeAskingAnything() {
        PortalChatEndpoint endpoint = endpointThatAnswers();

        assertEquals(415, endpoint.post("ANA", "text/plain", "{\"question\":\"hola\"}").status());
        assertEquals(415, endpoint.post("ANA", null, "{\"question\":\"hola\"}").status());
        assertEquals(415, endpoint.post("ANA", "application/x-www-form-urlencoded", "question=hola").status());
        assertTrue(questions.isEmpty());
    }

    @Test
    void aBodyThatIsTooBigIsRefused() {
        String body = "{\"question\":\"" + "x".repeat(PortalChatEndpoint.MAX_BODY_CHARS) + "\"}";

        assertEquals(413, endpointThatAnswers().post("ANA", JSON, body).status());
        assertTrue(questions.isEmpty());
    }

    @Test
    void aBodyWithoutAQuestionIsRefused() {
        PortalChatEndpoint endpoint = endpointThatAnswers();

        assertEquals(400, endpoint.post("ANA", JSON, "").status());
        assertEquals(400, endpoint.post("ANA", JSON, "no es json").status());
        assertEquals(400, endpoint.post("ANA", JSON, "[1,2]").status());
        assertEquals(400, endpoint.post("ANA", JSON, "{\"otra\":\"cosa\"}").status());
        assertEquals(400, endpoint.post("ANA", JSON, "{\"question\":5}").status());
        assertTrue(questions.isEmpty());
    }

    @Test
    void aBusinessErrorGoesBackWithItsMessage() {
        PortalChatEndpoint.Response limit = endpointThatFails(new BusinessRuleException("Hiciste muchas preguntas"))
                .post("ANA", JSON, "{\"question\":\"hola\"}");
        PortalChatEndpoint.Response invalid = endpointThatFails(new InvalidDataException("pregunta", "está vacía"))
                .post("ANA", JSON, "{\"question\":\"hola\"}");

        assertEquals(400, limit.status());
        assertEquals("Hiciste muchas preguntas", read(limit).get("error"));
        assertEquals(400, invalid.status());
        assertTrue(String.valueOf(read(invalid).get("error")).contains("pregunta"));
    }

    @Test
    void aFailureOfTheAiServiceIsAnUnavailableService() {
        PortalChatEndpoint.Response response = endpointThatFails(new AiException("El asistente está saturado"))
                .post("ANA", JSON, "{\"question\":\"hola\"}");

        assertEquals(503, response.status());
        assertEquals("El asistente está saturado", read(response).get("error"));
    }

    @Test
    void anUnexpectedFailureDoesNotShowItsDetails() {
        PortalChatEndpoint.Response response = endpointThatFails(new IllegalStateException("clave secreta 123"))
                .post("ANA", JSON, "{\"question\":\"hola\"}");

        assertEquals(500, response.status());
        assertFalse(response.json().contains("secreta"));
    }

    @Test
    void quotesAndLineBreaksInTheAnswerStayValidJson() {
        PortalChatEndpoint endpoint = new PortalChatEndpoint((token, question) -> "Dijo \"hola\"\ny se fue");

        PortalChatEndpoint.Response response = endpoint.post("ANA", JSON, "{\"question\":\"hola\"}");

        assertEquals("Dijo \"hola\"\ny se fue", read(response).get("answer"));
    }
}