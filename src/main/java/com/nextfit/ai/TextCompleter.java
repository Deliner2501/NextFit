package com.nextfit.ai;

/**
 * Something that answers one prompt with one text, without a conversation and without tools. It is
 * what a feature needs when it asks the AI for a piece of work (for example a draft of a routine)
 * and then checks the result itself. It lets that feature be tested with a lambda instead of the real AI.
 */
@FunctionalInterface
public interface TextCompleter {

    /**
     * Asks the AI for a text.
     *
     * @param system    the instructions for the AI
     * @param prompt    what is asked
     * @param maxTokens the most tokens the AI may write
     * @return the text that the AI wrote
     */
    String complete(String system, String prompt, int maxTokens);
}