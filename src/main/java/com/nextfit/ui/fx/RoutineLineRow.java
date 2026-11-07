package com.nextfit.ui.fx;

import com.nextfit.model.Exercise;
import com.nextfit.model.RoutineExercise;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Consumer;

/**
 * One exercise line of a routine day: the exercise, sets, repetitions, rest and a
 * button to remove the line. It only collects the values; the service validates them.
 */
final class RoutineLineRow extends HBox {

    private final ComboBox<Exercise> exerciseBox = new ComboBox<>();
    private final Spinner<Integer> sets = new Spinner<>(1, 10, 3);
    private final Spinner<Integer> repetitions = new Spinner<>(1, 100, 10);
    private final Spinner<Integer> rest = new Spinner<>(0, 600, 60, 15);

    /**
     * @param exercises the catalog the user can choose from
     * @param line      the line to show, or null for a new empty line
     * @param onRemove  action to run when the user presses "Quitar"
     */
    RoutineLineRow(List<Exercise> exercises, RoutineExercise line, Consumer<RoutineLineRow> onRemove) {
        super(10);
        setAlignment(Pos.BOTTOM_LEFT);

        exerciseBox.getItems().setAll(exercises);
        exerciseBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Exercise exercise) {
                return exercise == null ? "" : exercise.getName();
            }

            @Override
            public Exercise fromString(String text) {
                return null;
            }
        });
        exerciseBox.getStyleClass().add("nf-input");
        exerciseBox.setPrefWidth(230);
        exerciseBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(exerciseBox, Priority.ALWAYS);

        for (Spinner<Integer> spinner : List.of(sets, repetitions, rest)) {
            spinner.setEditable(true);
            spinner.setPrefWidth(95);
            spinner.getStyleClass().add("nf-input");
            commitOnFocusLost(spinner);
        }

        if (line != null) {
            for (Exercise exercise : exercises) {
                if (exercise.getId() == line.getExerciseId()) {
                    exerciseBox.getSelectionModel().select(exercise);
                }
            }
            sets.getValueFactory().setValue(line.getSets());
            repetitions.getValueFactory().setValue(line.getRepetitions());
            rest.getValueFactory().setValue(line.getRestSeconds());
        } else if (!exercises.isEmpty()) {
            exerciseBox.getSelectionModel().selectFirst();
        }

        Button remove = new Button("Quitar");
        remove.getStyleClass().add("nf-secondary");
        remove.setOnAction(e -> onRemove.accept(this));

        getChildren().addAll(
                FxDialogs.labeled("Ejercicio", exerciseBox),
                FxDialogs.labeled("Series", sets),
                FxDialogs.labeled("Reps", repetitions),
                FxDialogs.labeled("Descanso (s)", rest),
                remove);
    }

    // Si el usuario escribe un número y pasa a otro campo sin dar Enter, igual se guarda.
    private static void commitOnFocusLost(Spinner<Integer> spinner) {
        spinner.getEditor().focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                try {
                    spinner.getValueFactory().setValue(Integer.valueOf(spinner.getEditor().getText().trim()));
                } catch (NumberFormatException e) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
    }

    /**
     * Builds the domain line from what is on the screen.
     *
     * @return the routine line; the exercise id is 0 when no exercise is chosen,
     *         so the service rejects it with a clear message
     */
    RoutineExercise toRoutineExercise() {
        Exercise selected = exerciseBox.getValue();
        long exerciseId = selected == null ? 0 : selected.getId();
        return new RoutineExercise(exerciseId, sets.getValue(), repetitions.getValue(), rest.getValue());
    }
}