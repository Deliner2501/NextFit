package com.nextfit.ai;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Member;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The chat of the member portal. It answers a question from the phone of a member:
 * <ul>
 *   <li>the member is found with the access code of his carnet, never with what he writes;</li>
 *   <li>each member has his own conversation, which starts again after a rest, so nobody reads
 *       what another member asked and chats do not grow forever;</li>
 *   <li>each member may ask a limited number of questions per hour, so one person cannot spend
 *       the daily budget of the whole gym.</li>
 * </ul>
 */
public class PortalChatService {

    /** Questions that a member may ask in one hour. */
    public static final int DEFAULT_QUESTIONS_PER_HOUR = 20;

    /** Time without questions after which the conversation starts again. */
    public static final Duration IDLE_LIMIT = Duration.ofMinutes(30);

    private static final Duration WINDOW = Duration.ofHours(1);

    private final AiAssistant assistant;
    private final MemberByToken members;
    private final Clock clock;
    private final int questionsPerHour;
    private final Map<Long, Chat> chats = new HashMap<>();

    /**
     * Creates the service with the usual limit of questions.
     *
     * @param assistant the assistant that answers
     * @param members   finds the member of an access code
     * @param clock     the source of the current time
     */
    public PortalChatService(AiAssistant assistant, MemberByToken members, Clock clock) {
        this(assistant, members, clock, DEFAULT_QUESTIONS_PER_HOUR);
    }

    /**
     * Creates the service.
     *
     * @param assistant        the assistant that answers
     * @param members          finds the member of an access code
     * @param clock            the source of the current time
     * @param questionsPerHour how many questions a member may ask in one hour
     */
    public PortalChatService(AiAssistant assistant, MemberByToken members, Clock clock, int questionsPerHour) {
        this.assistant = assistant;
        this.members = members;
        this.clock = clock;
        this.questionsPerHour = questionsPerHour;
    }

    /**
     * Answers a question of the member that owns an access code.
     *
     * @param token    the access code of the carnet
     * @param question what the member wrote
     * @return the answer
     * @throws BusinessRuleException when the code belongs to nobody or the member asked too much
     * @throws com.nextfit.exceptions.NextFitException when the question or the AI service fails
     */
    public String ask(String token, String question) {
        Member member = members.find(token).orElseThrow(() -> new BusinessRuleException("Código no válido"));
        Chat chat = chatOf(member.getId());
        // EXPLICACIÓN: se bloquea solo el chat de este socio, así los demás no esperan su respuesta
        synchronized (chat) {
            countQuestion(chat);
            AiUser user = AiUser.member(member.getId(), firstName(member.getFullName()));
            String answer = assistant.ask(user, chat.conversation, question);
            chat.lastUsed = Instant.now(clock);
            return answer;
        }
    }

    private synchronized Chat chatOf(long memberId) {
        Instant now = Instant.now(clock);
        Chat chat = chats.computeIfAbsent(memberId, id -> new Chat(now));
        if (Duration.between(chat.lastUsed, now).compareTo(IDLE_LIMIT) > 0) {
            chat.conversation = new AiConversation();
        }
        chat.lastUsed = now;
        return chat;
    }

    // EXPLICACIÓN: cada intento cuenta, también el que falla, porque igual pudo costar dinero
    private void countQuestion(Chat chat) {
        Instant now = Instant.now(clock);
        while (!chat.asks.isEmpty() && Duration.between(chat.asks.peekFirst(), now).compareTo(WINDOW) >= 0) {
            chat.asks.pollFirst();
        }
        if (chat.asks.size() >= questionsPerHour) {
            throw new BusinessRuleException("Hiciste muchas preguntas seguidas. Intenta de nuevo en un rato");
        }
        chat.asks.addLast(now);
    }

    private static String firstName(String fullName) {
        String first = fullName.trim().split("\\s+")[0];
        return first.substring(0, 1).toUpperCase(Locale.ROOT) + first.substring(1).toLowerCase(Locale.ROOT);
    }

    /** The conversation of one member, when he last used it and when he asked in the last hour. */
    private static final class Chat {

        private AiConversation conversation = new AiConversation();
        private Instant lastUsed;
        private final Deque<Instant> asks = new ArrayDeque<>();

        Chat(Instant now) {
            this.lastUsed = now;
        }
    }
}