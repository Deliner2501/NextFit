package com.nextfit.ui.fx;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Exercise;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.Trainer;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainerService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Form to create or edit a routine: name, objective, trainer and one or more days,
 * each with its exercise lines. It builds a {@link Routine} and hands it to
 * {@link RoutineService}, which validates it; any error is shown inside the form.
 */
final class RoutineForm {

    private final RoutineService service;
    private final Routine existing;   // null cuando se crea una rutina nueva
    private final List<Exercise> exercises;
    private final List<Trainer> trainers;

    private final TextField nameField = new TextField();
    private final TextField objectiveField = new TextField();
    private final ComboBox<Trainer> trainerBox = new ComboBox<>();
    private final TabPane daysTabs = new TabPane();
    private final List<RoutineDayPane> dayPanes = new ArrayList<>();

    private RoutineForm(RoutineService service, TrainerService trainerService,
                        ExerciseService catalog, Routine existing) {
        this.service = service;
        this.existing = existing;
        this.exercises = catalog.findAll();
        this.trainers = trainerService.findAll().stream().filter(Trainer::isActive).toList();
    }

    /**
     * Opens the form in a dialog.
     *
     * @param owner          a node of the window the dialog belongs to
     * @param service        the routine service used to save
     * @param trainerService the service that lists the active trainers
     * @param catalog        the service that lists the exercises to choose from
     * @param existing       the routine to edit, or null to create a new one
     * @param onSaved        action to run after a successful save
     */
    static void open(Node owner, RoutineService service, TrainerService trainerService,
                     ExerciseService catalog, Routine existing, Runnable onSaved) {
        RoutineForm form = new RoutineForm(service, trainerService, catalog, existing);
        form.fillFromExisting();
        FxDialogs.form(owner, existing == null ? "Nueva rutina" : "Editar rutina", "Guardar rutina",
                form.buildFields(), () -> {
                    form.save();
                    onSaved.run();
                });
    }

    private Node buildFields() {
        nameField.setPromptText("Ej. Fuerza para principiantes");
        nameField.getStyleClass().add("nf-input");
        objectiveField.setPromptText("Ej. Ganar masa muscular");
        objectiveField.getStyleClass().add("nf-input");

        trainerBox.getItems().setAll(trainers);
        trainerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Trainer trainer) {
                return trainer == null ? "" : trainer.getFullName();
            }

            @Override
            public Trainer fromString(String text) {
                return null;
            }
        });
        trainerBox.getStyleClass().add("nf-input");
        trainerBox.setMaxWidth(Double.MAX_VALUE);

        Label daysLabel = new Label("Días de la rutina");
        daysLabel.getStyleClass().add("nf-field-label");
        HBox.setHgrow(daysLabel, Priority.ALWAYS);
        daysLabel.setMaxWidth(Double.MAX_VALUE);
        Button addDay = new Button("+ Agregar día");
        addDay.getStyleClass().add("nf-secondary");
        addDay.setOnAction(e -> addDay(null));
        Button removeDay = new Button("Quitar día");
        removeDay.getStyleClass().add("nf-secondary");
        removeDay.setOnAction(e -> removeSelectedDay());
        HBox daysHeader = new HBox(10, daysLabel, addDay, removeDay);
        daysHeader.setAlignment(Pos.CENTER_LEFT);

        daysTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        daysTabs.getStyleClass().add("nf-tabs");

        VBox fields = new VBox(14,
                FxDialogs.labeled("Nombre", nameField),
                FxDialogs.labeled("Objetivo", objectiveField),
                FxDialogs.labeled("Entrenador", trainerBox),
                daysHeader,
                daysTabs);
        fields.setPrefWidth(780);
        return fields;
    }

    // Al editar se cargan los datos actuales; al crear se empieza con un día vacío.
    private void fillFromExisting() {
        if (existing == null) {
            addDay(null);
            return;
        }
        // Estos campos se llenan antes de armar la pantalla; buildFields no los borra.
        nameField.setText(existing.getName());
        objectiveField.setText(existing.getObjective());
        for (Trainer trainer : trainers) {
            if (trainer.getId() == existing.getTrainerId()) {
                trainerBox.setValue(trainer);
            }
        }
        for (RoutineDay day : existing.getDays()) {
            addDay(day);
        }
        daysTabs.getSelectionModel().selectFirst();
    }

    private void addDay(RoutineDay day) {
        RoutineDayPane pane = new RoutineDayPane(exercises, day);
        dayPanes.add(pane);
        Tab tab = new Tab("Día " + dayPanes.size(), pane);
        tab.setClosable(false);
        daysTabs.getTabs().add(tab);
        daysTabs.getSelectionModel().select(tab);
    }

    private void removeSelectedDay() {
        int index = daysTabs.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            return;
        }
        dayPanes.remove(index);
        daysTabs.getTabs().remove(index);
        // Se renumeran las pestañas para que siempre digan Día 1, Día 2...
        for (int i = 0; i < daysTabs.getTabs().size(); i++) {
            daysTabs.getTabs().get(i).setText("Día " + (i + 1));
        }
    }

    // Arma la rutina y la guarda con el servicio; si hay error, FxDialogs lo muestra en rojo.
    private void save() {
        if (trainers.isEmpty()) {
            throw new BusinessRuleException("Primero registra un entrenador activo en la pestaña Entrenadores.");
        }
        Trainer trainer = trainerBox.getValue();
        if (trainer == null) {
            throw new BusinessRuleException("Elige el entrenador de la rutina.");
        }
        List<RoutineDay> days = new ArrayList<>();
        for (RoutineDayPane pane : dayPanes) {
            days.add(pane.toRoutineDay());
        }
        Routine routine = new Routine(
                existing == null ? 0 : existing.getId(),
                nameField.getText().trim(),
                objectiveField.getText().trim(),
                trainer.getId(),
                existing == null ? LocalDate.now() : existing.getCreatedAt(),
                days);
        if (existing == null) {
            service.create(routine);
        } else {
            service.update(routine);
        }
    }
}