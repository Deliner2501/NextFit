package com.nextfit.ui.fx;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.prefs.Preferences;

/**
 * Dark and light themes of the JavaFX interface. The colors live in the style
 * sheet ({@code nextfit.css}); this class only decides which mode is active,
 * remembers the choice between runs and applies it to the scene. Screens never
 * write a color in code, so a new mode only needs new values in the style sheet
 * (GRASP Protected Variations).
 */
public final class FxTheme {

    /** The two modes of the interface. */
    public enum Mode { DARK, LIGHT }

    private static final String PREFERENCE_KEY = "theme";
    private static final String STYLE_SHEET = "/fx/nextfit.css";
    /** Second layer: green and violet contrast and visual hierarchy. It loads after the first one. */
    private static final String VIOLET_SHEET = "/fx/nextfit-violet.css";

    private final Preferences preferences = Preferences.userNodeForPackage(FxTheme.class);
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>();
    private Scene scene;

    /** Creates the theme with the mode saved in the previous run (dark the first time). */
    public FxTheme() {
        mode.set("light".equals(preferences.get(PREFERENCE_KEY, "dark")) ? Mode.LIGHT : Mode.DARK);
    }

    /**
     * Returns the active mode as a property, so a button can follow its changes.
     *
     * @return the mode property
     */
    public ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    /**
     * Connects the theme to a scene and applies the active mode to it.
     *
     * @param target the only scene of the application
     */
    public void attach(Scene target) {
        this.scene = target;
        String css = FxTheme.class.getResource(STYLE_SHEET).toExternalForm();
        String violet = FxTheme.class.getResource(VIOLET_SHEET).toExternalForm();
        target.getStylesheets().setAll(css, violet);
        apply();
    }

    /**
     * Marks a root with the active mode. Call it every time the scene changes its root.
     *
     * @param root the root that is going to be shown
     */
    public void mark(Parent root) {
        root.getStyleClass().removeAll("dark", "light");
        root.getStyleClass().add(mode.get() == Mode.DARK ? "dark" : "light");
    }

    /** Switches between dark and light and remembers the choice. */
    public void toggle() {
        mode.set(mode.get() == Mode.DARK ? Mode.LIGHT : Mode.DARK);
        preferences.put(PREFERENCE_KEY, mode.get() == Mode.DARK ? "dark" : "light");
        apply();
    }

    private void apply() {
        String base = mode.get() == Mode.DARK
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet();
        Application.setUserAgentStylesheet(base);
        if (scene != null && scene.getRoot() != null) {
            mark(scene.getRoot());
        }
    }
}
