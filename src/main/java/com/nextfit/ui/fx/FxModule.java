package com.nextfit.ui.fx;

import com.nextfit.model.Role;
import javafx.scene.Node;

import java.util.List;

/**
 * Contract every JavaFX screen implements so the main window can host it in
 * the sidebar. Each team member delivers a module as an implementation, so adding a screen never requires
 * editing {@link ShellView} (Open/Closed).
 */
public interface FxModule {

    /**
     * Returns the stable identifier of the screen, e.g. "users".
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
     * Returns the SVG path of the sidebar icon (see {@link FxIcons}).
     *
     * @return the path data
     */
    String icon();

    /**
     * Returns the screen itself.
     *
     * @return the node to display
     */
    Node view();

    /** Called every time the user navigates to this screen, to refresh its data. */
    default void onShow() {
    }

    /**
     * Tells whether users with the given role may open this screen.
     *
     * @param role the role of the signed-in user
     * @return true when the screen must appear in the menu for that role
     */
    default boolean allowedFor(Role role) {
        return true;
    }

    /**
     * Extra words that lead to this module in the global search, such as the tabs it contains.
     *
     * @return the words, in Spanish like the screens
     */
    default List<String> keywords() {
        return List.of();
    }

    /**
     * Results this module adds to the global search. Modules without searchable content keep the default.
     *
     * @param query what the user typed
     * @return the matching results, at most a few
     */
    default List<SearchHit> search(String query) {
        return List.of();
    }

    /**
     * Opens something inside the module after the user chose one of its search results.
     *
     * @param targetId the identifier given by the module in its {@link SearchHit}
     */
    default void open(String targetId) {
    }
}
