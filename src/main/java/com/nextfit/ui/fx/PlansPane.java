package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.MembershipPlan;
import com.nextfit.service.MembershipPlanService;
import com.nextfit.ui.Formats;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

/**
 * Screen with the membership plans the gym sells: a table where the user can
 * create, edit, withdraw and put plans back on sale. The rules live in
 * {@link MembershipPlanService}; this class only collects clicks and shows data.
 */
final class PlansPane extends VBox {

    private static final String ON_SALE = "En venta";

    private final MembershipPlanService service;
    private final TableView<MembershipPlan> table = new TableView<>();
    private final Label countLabel = new Label();
    private final Button toggleButton = new Button("Retirar de la venta");

    /**
     * @param service the service that manages the plans
     */
    PlansPane(MembershipPlanService service) {
        super(16);
        this.service = service;
        getChildren().addAll(buildHeader(), buildTable());
        VBox.setVgrow(table, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Planes de membresía");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button edit = new Button("Editar");
        edit.getStyleClass().add("nf-secondary");
        edit.setOnAction(e -> editSelected());
        toggleButton.getStyleClass().add("nf-secondary");
        toggleButton.setOnAction(e -> toggleSelected());
        Button create = new Button("+ Nuevo plan");
        create.getStyleClass().add("nf-primary-small");
        create.setOnAction(e -> openForm(null));

        HBox header = new HBox(10, titles, edit, toggleButton, create);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildTable() {
        TableColumn<MembershipPlan, String> name = column("Plan", 200, MembershipPlan::getName);
        TableColumn<MembershipPlan, String> duration = column("Duración", 110, p -> Formats.days(p.getDurationDays()));
        TableColumn<MembershipPlan, String> price = column("Precio", 140, p -> Formats.money(p.getPrice()));
        TableColumn<MembershipPlan, String> description = column("Descripción", 360, MembershipPlan::getDescription);
        TableColumn<MembershipPlan, String> state = column("Estado", 130, p -> p.isActive() ? ON_SALE : "Retirado");
        state.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("nf-state-on", "nf-state-off");
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(value);
                    getStyleClass().add(ON_SALE.equals(value) ? "nf-state-on" : "nf-state-off");
                }
            }
        });
        table.getColumns().addAll(List.of(name, duration, price, description, state));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Aún no hay planes"));
        table.getStyleClass().add("nf-table");
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> updateToggleText());
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editSelected();
            }
        });
        table.setMinHeight(300);
        return table;
    }

    private static TableColumn<MembershipPlan, String> column(String title, double width,
                                                              Function<MembershipPlan, String> value) {
        TableColumn<MembershipPlan, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setSortable(false);
        return column;
    }

    /** Reloads the plans from the service. */
    void refresh() {
        MembershipPlan keep = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(service.findAll());
        if (keep != null) {
            for (MembershipPlan plan : table.getItems()) {
                if (plan.getId() == keep.getId()) {
                    table.getSelectionModel().select(plan);
                }
            }
        }
        int total = table.getItems().size();
        countLabel.setText(total == 1 ? "1 plan" : total + " planes");
        updateToggleText();
    }

    private void updateToggleText() {
        MembershipPlan selected = table.getSelectionModel().getSelectedItem();
        toggleButton.setText(selected != null && !selected.isActive() ? "Volver a vender" : "Retirar de la venta");
    }

    private MembershipPlan requireSelection() {
        MembershipPlan selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            FxDialogs.message(this, "Selecciona un plan", "Elige un plan de la tabla primero.");
        }
        return selected;
    }

    private void editSelected() {
        MembershipPlan selected = requireSelection();
        if (selected != null) {
            openForm(selected);
        }
    }

    private void toggleSelected() {
        MembershipPlan selected = requireSelection();
        if (selected == null) {
            return;
        }
        try {
            if (selected.isActive()) {
                service.deactivate(selected.getId());
            } else {
                service.reactivate(selected.getId());
            }
            refresh();
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
        }
    }

    private void openForm(MembershipPlan existing) {
        TextField nameField = input("Ej. Mensual", existing == null ? "" : existing.getName());
        TextField descriptionField = input("Ej. Acceso a todas las zonas", existing == null ? "" : existing.getDescription());
        TextField durationField = input("Días de vigencia", existing == null ? "" : String.valueOf(existing.getDurationDays()));
        TextField priceField = input("Precio en pesos", existing == null ? "" : String.valueOf(existing.getPrice()));

        HBox numbers = new HBox(12, FxDialogs.labeled("Duración (días)", durationField),
                FxDialogs.labeled("Precio ($)", priceField));
        for (Node box : numbers.getChildren()) {
            HBox.setHgrow(box, Priority.ALWAYS);
        }
        VBox fields = new VBox(12, FxDialogs.labeled("Nombre", nameField),
                FxDialogs.labeled("Descripción", descriptionField), numbers);

        FxDialogs.form(this, existing == null ? "Nuevo plan" : "Editar plan", "Guardar", fields, () -> {
            MembershipPlan plan = new MembershipPlan(
                    existing == null ? 0 : existing.getId(),
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    Formats.parseWholeNumber("duración", durationField.getText()),
                    Formats.parseMoney("precio", priceField.getText()),
                    existing == null || existing.isActive());
            if (existing == null) {
                service.create(plan);
            } else {
                service.update(plan);
            }
            refresh();
        });
    }

    private static TextField input(String prompt, String text) {
        TextField field = new TextField(text);
        field.setPromptText(prompt);
        field.getStyleClass().add("nf-input");
        field.setPrefHeight(44);
        return field;
    }
}
