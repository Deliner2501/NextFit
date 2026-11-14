package com.nextfit.ai;

/**
 * The door to an AI service. The assistant only knows this interface, so the real service
 * (Claude over the internet) and a fake one for the tests are the same thing for it, and
 * changing the provider does not change the assistant.
 */
@FunctionalInterface
public interface AiClient {

    /**
     * Sends a request and waits for the answer.
     *
     * @param request the instructions, the conversation and the tools
     * @return the answer
     * @throws com.nextfit.exceptions.AiException when the service cannot answer
     */
    AiReply send(AiRequest request);
}