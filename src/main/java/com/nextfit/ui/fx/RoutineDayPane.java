package com.nextfit.ui.fx;

import com.nextfit.model.Exercise;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * One day of a routine: a name and a list of exercise lines the user can add or
 * remove. It only collects data; the rules are checked by the routine service.
 */
final class RoutineDayPane extends VBox {

    private final List<Exercise> exercises;
    private final TextField nameField = new TextField();
    private final VBox rowsBox = new VBox(12);
    private final List<RoutineLineRow> rows = new ArrayList<>();

    /**
     * @param exercises the catalog the user can choose from
     * @param day       the day to show, or null for a new day with one empty line
     */
    RoutineDayPane(List<Exercise> exercises, RoutineDay day) {
        super(12);
        this.exercises = exercises;
        setPadding(new Insets(14, 0, 0, 0));

        nameField.setPromptText("Nombre del día, ej. Pecho y tríceps");
        nameField.getStyleClass().add("nf-input");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        Button addExercise = new Button("+ Ejercicio");
        addExercise.getStyleClass().add("nf-secondary");
        addExercise.setOnAction(e -> addRow(null));
        HBox top = new HBox(10, nameField, addExercise);
        top.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(230);
        scroll.getStyleClass().add("nf-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(top, scroll);

        if (day == null) {
            addRow(null);
        } else {
            nameField.setText(day.getName());
            for (RoutineExercise line : day.getExercises()) {
                addRow(line);
            }
        }
    }

    private void addRow(RoutineExercise line) {
        RoutineLineRow row = new RoutineLineRow(exercises, line, this::removeRow);
        rows.add(row);
        rowsBox.getChildren().add(row);
    }

    private void removeRow(RoutineLineRow row) {
        rows.remove(row);
        rowsBox.getChildren().remove(row);
    }

    /**
     * Builds the domain day from what is on the screen.
     *
     * @return the routine day with its exercise lines
     */
    RoutineDay toRoutineDay() {
        List<RoutineExercise> lines = new ArrayList<>();
        for (RoutineLineRow row : rows) {
            lines.add(row.toRoutineExercise());
        }
        return new RoutineDay(nameField.getText().trim(), lines);
    }
}