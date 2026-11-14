package com.nextfit.ai;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What the AI answered in one call, with how many tokens the call used so the cost can be limited.
 *
 * @param blocks       the pieces of the answer: text and requests for tools
 * @param stopReason   why it stopped, as the service says it
 * @param inputTokens  tokens that were sent
 * @param outputTokens tokens that were written
 */
public record AiReply(List<AiBlock> blocks, String stopReason, int inputTokens, int outputTokens) {

    /** The AI finished what it wanted to say. */
    public static final String END_TURN = "end_turn";

    /** The AI stopped because it wants to run a tool. */
    public static final String TOOL_USE = "tool_use";

    /** The AI stopped because it reached the limit of tokens. */
    public static final String MAX_TOKENS = "max_tokens";

    /** Keeps its own copy of the blocks. */
    public AiReply {
        blocks = List.copyOf(blocks);
    }

    /**
     * Returns the text of the answer, with its paragraphs joined.
     *
     * @return the text, empty when the AI only asked for tools
     */
    public String text() {
        return blocks.stream()
                .filter(block -> block instanceof AiBlock.Text)
                .map(block -> ((AiBlock.Text) block).text())
                .collect(Collectors.joining("\n"))
                .trim();
    }

    /**
     * Returns the tools the AI asked to run.
     *
     * @return the requests, in order
     */
    public List<AiBlock.ToolUse> toolUses() {
        return blocks.stream()
                .filter(block -> block instanceof AiBlock.ToolUse)
                .map(block -> (AiBlock.ToolUse) block)
                .toList();
    }
}