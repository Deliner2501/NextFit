package com.nextfit.ui;

import com.nextfit.model.Role;

import javax.swing.JComponent;

/**
 * Contract every feature screen implements so the main window can host
 * it in the sidebar. Each team member delivers their modules as
 * ModulePanel implementations, so adding a screen never requires
 * editing the main window.
 */
public interface ModulePanel {

    /**
     * Returns the stable identifier of the screen, e.g. "members".
     *
     * @return the identifier
     */
    String id();

    /**
     * Returns the text shown in the sidebar (Spanish).
     *
     * @return the title
     */
    String title();

    /**
     * Returns the name of the icon, without extension (see {@code Icons}).
     *
     * @return the icon name
     */
    String icon();

    /**
     * Returns the screen itself.
     *
     * @return the component to display
     */
    JComponent view();

    /** Called every time the user navigates to this screen, to refresh its data. */
    default void onShow() {
    }

    /**
     * Tells whether users with the given role may open this screen. By default
     * every role may; a module that is restricted overrides this method.
     *
     * @param role the role of the signed-in user
     * @return true when the screen must appear in the menu for that role
     */
    default boolean allowedFor(Role role) {
        return true;
    }
}