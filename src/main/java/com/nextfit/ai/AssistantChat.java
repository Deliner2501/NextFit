package com.nextfit.ai;

import java.util.function.Supplier;

/**
 * The chat of the assistant panel. It keeps the conversation between questions, and starts a new
 * one when the person who signed in is not the same as before, so nobody reads what another user
 * asked. The screen only shows what this class answers.
 */
public class AssistantChat {

    private final AiAssistant assistant;
    private final Supplier<AiUser> currentUser;
    private AiConversation conversation = new AiConversation();
    private AiUser lastUser;

    /**
     * Creates the chat.
     *
     * @param assistant   the assistant that answers
     * @param currentUser who is signed in now, asked on every question
     */
    public AssistantChat(AiAssistant assistant, Supplier<AiUser> currentUser) {
        this.assistant = assistant;
        this.currentUser = currentUser;
    }

    /**
     * Asks a question inside the current conversation. It may take seconds, so the screen must
     * call it outside the interface thread.
     *
     * @param question what the person wrote
     * @return the answer
     * @throws com.nextfit.exceptions.NextFitException when the question or the service fails
     */
    public String ask(String question) {
        AiUser user = currentUser.get();
        AiConversation chat;
        synchronized (this) {
            if (!user.equals(lastUser)) {
                conversation = new AiConversation();
                lastUser = user;
            }
            chat = conversation;
        }
        return assistant.ask(user, chat, question);
    }

    /** Forgets the conversation so far. */
    public synchronized void startOver() {
        conversation = new AiConversation();
    }
}