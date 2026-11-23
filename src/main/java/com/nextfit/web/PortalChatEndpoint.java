package com.nextfit.web;

import com.nextfit.ai.Json;
import com.nextfit.ai.PortalChatService.DailyQuota;
import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.NextFitException;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.LongFunction;

/**
 * What happens when the phone of a member sends a question to the chat of the portal. It checks
 * the request, asks the chat and builds the answer; the web server only moves bytes. The member is
 * identified with the access code of the URL, never with what the request says, and every failure
 * becomes a short message in Spanish that the page can show.
 */
public final class PortalChatEndpoint {

    /** Most characters of the body of a request; a question is short, so a big body is refused. */
    public static final int MAX_BODY_CHARS = 4000;

    private static final String GENERIC_ERROR = "No pude responder en este momento. Intenta de nuevo";

    /**
     * What the web server must answer.
     *
     * @param status the HTTP status
     * @param json   the body, a JSON with {@code answer} or {@code error}
     */
    public record Response(int status, String json) {
    }

    private final BiFunction<String, String, String> asker;
    private final LongFunction<DailyQuota> quotas;

    /**
     * Creates the endpoint.
     *
     * @param asker answers a question: it receives the access code and the question, and returns the answer
     */
    public PortalChatEndpoint(BiFunction<String, String, String> asker) {
        this(asker, memberId -> null);
    }

    /**
     * @param asker answers a question using the member's access code
     * @param quotas supplies the daily quota for the authenticated member
     */
    public PortalChatEndpoint(BiFunction<String, String, String> asker, LongFunction<DailyQuota> quotas) {
        this.asker = asker;
        this.quotas = quotas;
    }

    /**
     * @param memberId the authenticated member
     * @return the daily quota, when tracking is configured
     */
    public Optional<DailyQuota> dailyQuota(long memberId) {
        return Optional.ofNullable(quotas.apply(memberId));
    }

    /**
     * Answers a question sent from the phone.
     *
     * @param token       the access code taken from the address of the request
     * @param contentType the content type header of the request
     * @param body        the body of the request, a JSON like {@code {"question":"..."}}
     * @return the status and the JSON to send back
     */
    public Response post(String token, String contentType, String body) {
        // EXPLICACIÓN: exigir application/json obliga a otras páginas web a pedir permiso (CORS) antes de enviar
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
            return error(415, "Formato no válido");
        }
        if (body == null || body.length() > MAX_BODY_CHARS) {
            return error(413, "La pregunta es demasiado larga");
        }
        String question;
        try {
            question = questionOf(Json.parse(body));
        } catch (AiException e) {
            return error(400, "Pregunta no válida");
        }
        if (question == null) {
            return error(400, "Pregunta no válida");
        }
        try {
            return new Response(200, Json.write(Map.of("answer", asker.apply(token, question))));
        } catch (AiException e) {
            return error(503, e.getMessage());
        } catch (NextFitException e) {
            return error(400, e.getMessage());
        } catch (RuntimeException e) {
            // EXPLICACIÓN: un error inesperado no se muestra, podría traer datos internos
            return error(500, GENERIC_ERROR);
        }
    }

    private static String questionOf(Object parsed) {
        if (parsed instanceof Map<?, ?> map && map.get("question") instanceof String question) {
            return question;
        }
        return null;
    }

    private static Response error(int status, String message) {
        return new Response(status, Json.write(Map.of("error", message)));
    }
}