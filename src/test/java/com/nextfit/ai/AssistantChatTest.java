package com.nextfit.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantChatTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneOffset.UTC);

    private final List<AiRequest> requests = new ArrayList<>();
    private AiUser user;
    private AssistantChat chat;

    @BeforeEach
    void setUp() {
        user = new AiUser(AiAudience.ADMIN, 0, "admin");
        AiClient client = request -> {
            requests.add(request);
            return new AiReply(List.of(new AiBlock.Text("ok")), AiReply.END_TURN, 10, 5);
        };
        AiAssistant assistant = new AiAssistant(client, List.of(), new DailyTokenBudget(100_000, CLOCK), CLOCK);
        chat = new AssistantChat(assistant, () -> user);
    }

    private int messagesOfTheLastCall() {
        return requests.get(requests.size() - 1).messages().size();
    }

    @Test
    void theSamePersonKeepsTheConversation() {
        chat.ask("primera");
        chat.ask("segunda");

        assertEquals(3, messagesOfTheLastCall());
    }

    @Test
    void anotherPersonStartsANewConversation() {
        chat.ask("primera");
        user = new AiUser(AiAudience.RECEPTION, 0, "recepcion");

        chat.ask("hola");

        assertEquals(1, messagesOfTheLastCall());
    }

    @Test
    void startingOverForgetsTheConversation() {
        chat.ask("primera");
        chat.startOver();

        chat.ask("segunda");

        assertEquals(1, messagesOfTheLastCall());
    }

    @Test
    void returnsTheAnswer() {
        assertEquals("ok", chat.ask("hola"));
    }
}