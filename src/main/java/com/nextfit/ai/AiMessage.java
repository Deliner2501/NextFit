package com.nextfit.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * One turn of the conversation: what the user said or what the AI answered. The results of the
 * tools travel in a message of the user, as the AI service expects.
 *
 * @param role   {@link #USER} or {@link #ASSISTANT}
 * @param blocks the pieces of the message
 */
public record AiMessage(String role, List<AiBlock> blocks) {

    /** Role of what a person writes, and of the results of the tools. */
    public static final String USER = "user";

    /** Role of what the AI answers. */
    public static final String ASSISTANT = "assistant";

    /** Keeps its own copy of the blocks, so nobody can change the conversation later. */
    public AiMessage {
        blocks = List.copyOf(blocks);
    }

    /**
     * Creates a message of the user with only text.
     *
     * @param text what the user wrote
     * @return the message
     */
    public static AiMessage user(String text) {
        return new AiMessage(USER, List.of(new AiBlock.Text(text)));
    }

    /**
     * Creates a message with what the AI answered, as it came.
     *
     * @param blocks the blocks of the answer
     * @return the message
     */
    public static AiMessage assistant(List<AiBlock> blocks) {
        return new AiMessage(ASSISTANT, blocks);
    }

    /**
     * Creates the message that gives back the results of the tools the AI asked for.
     *
     * @param results the results, one for each request
     * @return the message
     */
    public static AiMessage toolResults(List<AiBlock.ToolResult> results) {
        return new AiMessage(USER, new ArrayList<AiBlock>(results));
    }
}