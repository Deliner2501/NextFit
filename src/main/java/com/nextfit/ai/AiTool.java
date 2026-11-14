package com.nextfit.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Something the AI can ask NextFit to look up, such as "the members who are about to leave".
 * A tool only reads: it never changes data. Every tool is a class of its own, so adding one
 * does not change the assistant, and each tool says which audiences may use it, so a trainer
 * never gets the income and a member only gets his own data.
 */
public interface AiTool {

    /**
     * Returns the name the AI uses to ask for the tool.
     *
     * @return a short name without spaces
     */
    String name();

    /**
     * Returns what the tool does and when to use it, written for the AI.
     *
     * @return the description
     */
    String description();

    /**
     * Returns the arguments the tool accepts, as a JSON schema.
     *
     * @return the schema, see {@link #noInput()} for a tool without arguments
     */
    Map<String, Object> inputSchema();

    /**
     * Says whether an audience may use the tool.
     *
     * @param audience the kind of person who asks
     * @return true when the tool is available for that audience
     */
    boolean allowedFor(AiAudience audience);

    /**
     * Runs the tool.
     *
     * @param input the arguments the AI gave
     * @param user  who is asking, to answer only what that person may see
     * @return what was found, as text for the AI to read
     */
    String run(Map<String, Object> input, AiUser user);

    /**
     * Returns the schema of a tool that takes no arguments.
     *
     * @return the schema
     */
    static Map<String, Object> noInput() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<String, Object>());
        return schema;
    }
}