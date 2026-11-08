package com.nextfit.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A key-number card: a round badge with an icon, a caption, the number and a
 * hint, with a big faint icon in the corner. The {@link Kind} decides its
 * weight on the screen, so the screens can give some numbers more importance.
 */
final class KpiCard extends StackPane {

    /** How important the card is. */
    enum Kind {
        /** The protagonist: bigger number, violet background, no corner icon. */
        FEATURE,
        /** A regular card with violet accents. */
        VIOLET,
        /** A regular card with a green glow in the corner. */
        LIME
    }

    private final Label value = new Label();
    private final Label hintLabel = new Label();

    /**
     * Creates the card.
     *
     * @param caption      what the number measures
     * @param initialValue the text of the number (it can be changed later with {@link #value()})
     * @param hint         a short note under the number
     * @param icon         the SVG path of the icon
     * @param kind         the weight of the card
     */
    KpiCard(String caption, String initialValue, String hint, String icon, Kind kind) {
        boolean feature = kind == Kind.FEATURE;
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("nf-muted");
        value.setText(initialValue);
        value.getStyleClass().add(feature ? "nf-kpi-xl" : "nf-kpi-value-small");
        hintLabel.setText(hint);
        hintLabel.getStyleClass().add("nf-muted-small");
        hintLabel.setWrapText(true);
        VBox texts = new VBox(4, captionLabel, value, hintLabel);
        HBox.setHgrow(texts, Priority.ALWAYS);

        double badgeSize = feature ? 56 : 52;
        StackPane badge = new StackPane(FxIcons.of(icon, feature ? 26 : 24));
        badge.getStyleClass().add(feature ? "nf-badge-solid" : "nf-badge-violet");
        badge.setMinSize(badgeSize, badgeSize);
        badge.setMaxSize(badgeSize, badgeSize);
        HBox content = new HBox(16, badge, texts);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(feature ? 26 : 18, 22, feature ? 26 : 18, 22));
        getChildren().add(content);

        if (feature) {
            getStyleClass().add("nf-card-feature");
        } else {
            getStyleClass().addAll("nf-kpi-card", kind == Kind.LIME ? "nf-kpi-card-lime" : "nf-kpi-card-violet");
            Region deco = FxIcons.of(icon, 84);
            deco.getStyleClass().add(kind == Kind.LIME ? "nf-kpi-deco" : "nf-kpi-deco-violet");
            deco.setOpacity(0.10);
            deco.setMouseTransparent(true);
            StackPane.setAlignment(deco, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(deco, new Insets(0, 14, 10, 0));
            getChildren().add(0, deco);
        }
        FxMotion.hoverLift(this, 1.02);
    }

    /**
     * A small rounded label with an arrow that says a number went up (green) or down (red).
     *
     * @param text    the change, for example "+12,5 %"
     * @param up      {@code true} for an increase, {@code false} for a decrease
     * @param tooltip what the change is compared with, shown when the mouse rests on it
     * @return the chip
     */
    static Label chip(String text, boolean up, String tooltip) {
        Label chip = new Label((up ? "↑ " : "↓ ") + text);
        chip.getStyleClass().add(up ? "nf-chip-up" : "nf-chip-down");
        Tooltip.install(chip, new Tooltip(tooltip));
        return chip;
    }

    /**
     * Puts a change chip in the top-right corner of the card.
     *
     * @param text    the change, for example "+2"
     * @param up      {@code true} for an increase, {@code false} for a decrease
     * @param tooltip what the change is compared with
     */
    void trend(String text, boolean up, String tooltip) {
        Label chip = chip(text, up, tooltip);
        StackPane.setAlignment(chip, Pos.TOP_RIGHT);
        StackPane.setMargin(chip, new Insets(14, 16, 0, 0));
        getChildren().add(chip);
        FxMotion.popIn(chip, 900);
    }

    /**
     * Gives access to the label of the number, so a screen can animate it.
     *
     * @return the label of the number
     */
    Label value() {
        return value;
    }

    /**
     * Gives access to the note under the number, so a screen can change it.
     *
     * @return the label of the hint
     */
    Label hint() {
        return hintLabel;
    }
}
