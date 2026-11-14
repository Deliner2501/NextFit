package com.nextfit.ai;

import java.util.List;

/**
 * What is sent to the AI in one call: the instructions, the conversation so far, the tools it
 * may ask for and the most it may write.
 *
 * @param system    the instructions that set the role and the limits of the assistant
 * @param messages  the conversation so far
 * @param tools     the tools it may ask for, possibly none
 * @param maxTokens the most tokens the answer may have, which also limits the cost
 */
public record AiRequest(String system, List<AiMessage> messages, List<AiTool> tools, int maxTokens) {

    /** Keeps its own copies of the lists. */
    public AiRequest {
        messages = List.copyOf(messages);
        tools = List.copyOf(tools);
    }
}