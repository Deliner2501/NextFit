package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Trainer;
import com.nextfit.service.TrainerService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Screen with the trainers of the gym: a table where the user can register, edit,
 * deactivate and reactivate them. The rules live in {@link TrainerService}; this
 * class only collects clicks and shows data.
 */
final class TrainersPane extends VBox {

    private final TrainerService service;
    private final TableView<Trainer> table = new TableView<>();
    private final Label countLabel = new Label();
    private final Button toggleButton = new Button("Desactivar");

    /** @param service the service that manages the trainers */
    TrainersPane(TrainerService service) {
        super(16);
        this.service = service;
        getChildren().addAll(buildHeader(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Entrenadores");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button edit = new Button("Editar");
        edit.getStyleClass().add("nf-secondary");
        edit.setOnAction(e -> editSelected());
        toggleButton.getStyleClass().add("nf-secondary");
        toggleButton.setOnAction(e -> toggleSelected());
        Button create = new Button("+ Nuevo entrenador");
        create.getStyleClass().add("nf-primary-small");
        create.setOnAction(e -> openForm(null));

        HBox header = new HBox(10, titles, edit, toggleButton, create);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildTable() {
        TableColumn<Trainer, String> name = FxTables.column("Nombre", 260, Trainer::getFullName);
        TableColumn<Trainer, String> document = FxTables.column("Documento", 160, Trainer::getDocumentNumber);
        TableColumn<Trainer, String> phone = FxTables.column("Teléfono", 160, Trainer::getPhone);
        TableColumn<Trainer, String> specialty = FxTables.column("Especialidad", 200, Trainer::getSpecialty);
        TableColumn<Trainer, String> state = FxTables.column("Estado", 120,
                t -> t.isActive() ? "Activo" : "Inactivo");
        FxTables.colorize(state, text -> "Activo".equals(text) ? "nf-state-on" : "nf-state-off");
        table.getColumns().addAll(List.of(name, document, phone, specialty, state));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Aún no hay entrenadores. Registra el primero con «+ Nuevo entrenador»."));
        table.getStyleClass().add("nf-table");
        // El botón cambia de texto según el estado del entrenador elegido.
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> updateToggleText());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editSelected();
            }
        });
        table.setMinHeight(320);
        return table;
    }

    /** Reloads the trainers from the service, keeping the selected one if it is still listed. */
    void refresh() {
        Trainer previous = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(service.findAll());
        if (previous != null) {
            for (Trainer trainer : table.getItems()) {
                if (trainer.getId() == previous.getId()) {
                    table.getSelectionModel().select(trainer);
                }
            }
        }
        int total = table.getItems().size();
        countLabel.setText(total == 1 ? "1 entrenador" : total + " entrenadores");
        updateToggleText();
    }

    private void updateToggleText() {
        Trainer selected = table.getSelectionModel().getSelectedItem();
        toggleButton.setText(selected != null && !selected.isActive() ? "Reactivar" : "Desactivar");
    }

    private void editSelected() {
        Trainer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona un entrenador", "Elige un entrenador de la tabla primero.");
        } else {
            openForm(selected);
        }
    }

    // Desactiva o reactiva al entrenador elegido; si una regla lo rechaza, se muestra el aviso.
    private void toggleSelected() {
        Trainer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona un entrenador", "Elige un entrenador de la tabla primero.");
            return;
        }
        try {
            if (selected.isActive()) {
                service.deactivate(selected.getId());
            } else {
                service.reactivate(selected.getId());
            }
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
        }
        refresh();
    }

    // Abre el formulario para registrar (trainer null) o editar un entrenador.
    private void openForm(Trainer trainer) {
        TrainerForm.open(this, service, trainer, this::refresh);
    }
}