package com.nextfit.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A piece of a message in the conversation with the AI: plain text, a request of the AI to
 * run a tool, or the result of that tool. They are plain values with no behavior.
 */
public sealed interface AiBlock {

    /**
     * Plain text, written by the user or by the AI.
     *
     * @param text the text
     */
    record Text(String text) implements AiBlock {
    }

    /**
     * The AI asks to run one of the tools with some arguments.
     *
     * @param id    the identifier that the answer of the tool must carry back
     * @param name  the name of the tool
     * @param input the arguments, as read from the JSON
     */
    record ToolUse(String id, String name, Map<String, Object> input) implements AiBlock {

        /** Keeps its own read-only copy of the arguments. */
        public ToolUse {
            input = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(input));
        }
    }

    /**
     * The answer of a tool to a request of the AI.
     *
     * @param toolUseId the identifier of the request that is answered
     * @param content   what the tool found, or the reason it failed
     * @param error     true when the tool could not do what was asked
     */
    record ToolResult(String toolUseId, String content, boolean error) implements AiBlock {
    }
}