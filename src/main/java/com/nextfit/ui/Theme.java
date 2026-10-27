package com.nextfit.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Map;

/**
 * Single source of colors and look-and-feel settings for the whole application.
 */
public final class Theme {

    public static final Color BACKGROUND = Color.decode("#0E1014");
    public static final Color SURFACE = Color.decode("#171A21");
    public static final Color SURFACE_ALT = Color.decode("#1F232C");
    public static final Color ACCENT = Color.decode("#B6FF3B");
    public static final Color ACCENT_ALT = Color.decode("#FF5A36");
    public static final Color TEXT = Color.decode("#E8ECF1");
    public static final Color TEXT_MUTED = Color.decode("#8B93A1");
    public static final Color SUCCESS = Color.decode("#3DDC97");
    public static final Color WARNING = Color.decode("#FFB020");
    public static final Color DANGER = Color.decode("#FF4D4F");

    private Theme() {
    }

    /** Installs the dark look-and-feel; call once before creating any window. */
    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "@accentColor", "#B6FF3B",
                "@background", "#0E1014"));
        FlatDarkLaf.setup();
        UIManager.put("Component.arc", 14);
        UIManager.put("Button.arc", 14);
        UIManager.put("TextComponent.arc", 14);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
    }
}