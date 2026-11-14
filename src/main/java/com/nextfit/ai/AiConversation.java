package com.nextfit.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The messages of one chat with the assistant. Each chat window has its own, so what one person
 * asked is never sent in the conversation of another.
 */
public class AiConversation {

    private final List<AiMessage> messages = new ArrayList<>();

    /**
     * Adds a message at the end.
     *
     * @param message the message
     */
    public void add(AiMessage message) {
        messages.add(message);
    }

    /**
     * Returns the messages so far.
     *
     * @return a read-only view, oldest first
     */
    public List<AiMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Returns how many messages there are.
     *
     * @return the number of messages
     */
    public int size() {
        return messages.size();
    }

    /**
     * Removes the messages after a point, to leave the chat as it was when a question failed.
     *
     * @param size the number of messages to keep
     */
    public void rollbackTo(int size) {
        while (messages.size() > size) {
            messages.remove(messages.size() - 1);
        }
    }
}