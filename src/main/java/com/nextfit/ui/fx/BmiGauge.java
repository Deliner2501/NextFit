package com.nextfit.ui.fx;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A colored bar with four stretches (underweight, normal, overweight, obesity) and a
 * marker that points to the BMI of the member. The arithmetic lives in {@link BmiScale}.
 */
final class BmiGauge extends VBox {

    private static final String[] NAMES = {"Bajo", "Normal", "Sobrepeso", "Obesidad"};
    private static final String[] COLORS = {"-nf-warning", "-nf-success", "-nf-warning", "-nf-danger"};
    private static final double MARKER_WIDTH = 6;

    private final StackPane track = new StackPane();
    private final Region marker = new Region();
    private double position = -1;

    BmiGauge() {
        super(6);
        GridPane bar = grid();
        GridPane names = grid();
        int last = BmiScale.segments() - 1;
        for (int i = 0; i <= last; i++) {
            Region segment = new Region();
            segment.setMinHeight(12);
            segment.setPrefHeight(12);
            segment.setMaxWidth(Double.MAX_VALUE);
            String radius = i == 0 ? "6 0 0 6" : i == last ? "0 6 6 0" : "0";
            segment.setStyle("-fx-background-color: " + COLORS[i] + "; -fx-background-radius: " + radius + ";");
            bar.add(segment, i, 0);

            Label name = new Label(NAMES[i]);
            name.getStyleClass().add("nf-muted-small");
            names.add(name, i, 0);
        }

        marker.setMinSize(MARKER_WIDTH, 28);
        marker.setPrefSize(MARKER_WIDTH, 28);
        marker.setMaxSize(MARKER_WIDTH, 28);
        marker.setStyle("-fx-background-color: -nf-text; -fx-background-radius: 3;"
                + " -fx-border-color: -nf-surface; -fx-border-radius: 3; -fx-border-width: 1;");
        marker.setVisible(false);
        StackPane.setAlignment(marker, Pos.CENTER_LEFT);
        track.getChildren().addAll(bar, marker);
        track.widthProperty().addListener((obs, old, now) -> place());

        getChildren().addAll(track, names);
    }

    /**
     * Moves the marker to a BMI, or hides it when there is none.
     *
     * @param bmi the body mass index; null when it cannot be calculated yet
     */
    void setBmi(Double bmi) {
        if (bmi == null) {
            position = -1;
            marker.setVisible(false);
            return;
        }
        position = BmiScale.position(bmi);
        marker.setVisible(true);
        place();
    }

    private void place() {
        if (position < 0) {
            return;
        }
        marker.setTranslateX(track.getWidth() * position - MARKER_WIDTH / 2);
    }

    // A row of four columns whose widths follow the size of each stretch of the scale.
    private static GridPane grid() {
        GridPane grid = new GridPane();
        for (int i = 0; i < BmiScale.segments(); i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(BmiScale.share(i) * 100);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }
}
