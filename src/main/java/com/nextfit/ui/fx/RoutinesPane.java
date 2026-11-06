package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Routine;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainerService;
import com.nextfit.ui.Formats;
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
 * Screen with the routines of the gym: a table where the user can create and edit
 * routines. The rules live in {@link RoutineService}; this class only collects clicks
 * and shows data.
 */
final class RoutinesPane extends VBox {

    private final RoutineService service;
    private final TrainerService trainers;
    private final TableView<Routine> table = new TableView<>();
    private final Label countLabel = new Label();

    /**
     * @param service  the service that manages the routines
     * @param trainers the service used to show the name of each trainer
     */
    RoutinesPane(RoutineService service, TrainerService trainers) {
        super(16);
        this.service = service;
        this.trainers = trainers;
        getChildren().addAll(buildHeader(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Rutinas");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button edit = new Button("Editar");
        edit.getStyleClass().add("nf-secondary");
        edit.setOnAction(e -> editSelected());
        Button create = new Button("+ Nueva rutina");
        create.getStyleClass().add("nf-primary-small");
        create.setOnAction(e -> openForm(null));

        HBox header = new HBox(10, titles, edit, create);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildTable() {
        TableColumn<Routine, String> name = FxTables.column("Nombre", 260, Routine::getName);
        TableColumn<Routine, String> objective = FxTables.column("Objetivo", 200, Routine::getObjective);
        TableColumn<Routine, String> trainer = FxTables.column("Entrenador", 220, this::trainerName);
        TableColumn<Routine, String> days = FxTables.column("Días", 90, r -> String.valueOf(r.getDays().size()));
        TableColumn<Routine, String> sets = FxTables.column("Series totales", 140,
                r -> String.valueOf(r.getTotalSets()));
        TableColumn<Routine, String> created = FxTables.column("Creada", 140,
                r -> Formats.date(r.getCreatedAt()));
        table.getColumns().addAll(List.of(name, objective, trainer, days, sets, created));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Aún no hay rutinas. Crea la primera con «+ Nueva rutina»."));
        table.getStyleClass().add("nf-table");
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editSelected();
            }
        });
        table.setMinHeight(320);
        return table;
    }

    /** Reloads the routines from the service. */
    void refresh() {
        table.getItems().setAll(service.findAll());
        int total = table.getItems().size();
        countLabel.setText(total == 1 ? "1 rutina" : total + " rutinas");
    }

    // Si el entrenador de la rutina ya no existe, se muestra un guion en vez de fallar.
    private String trainerName(Routine routine) {
        try {
            return trainers.findById(routine.getTrainerId()).getFullName();
        } catch (NextFitException e) {
            return "-";
        }
    }

    private void editSelected() {
        Routine selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona una rutina", "Elige una rutina de la tabla primero.");
        } else {
            openForm(selected);
        }
    }

    // Abre el formulario para crear (routine null) o editar una rutina.
    private void openForm(Routine routine) {
        FxDialogs.message(this, "Próximamente", "El formulario de rutinas se agrega en el siguiente paso.");
    }
}