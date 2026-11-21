package com.nextfit.ui.fx;

/**
 * One result of the global search.
 *
 * @param moduleId the module that must be opened when the result is chosen
 * @param targetId what to open inside the module, or an empty text for the module itself
 * @param title    the main line shown to the user
 * @param detail   the secondary line shown to the user
 */
public record SearchHit(String moduleId, String targetId, String title, String detail) {
}
