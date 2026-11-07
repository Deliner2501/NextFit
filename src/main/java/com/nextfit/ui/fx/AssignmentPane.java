package com.nextfit.ui.fx;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.service.MemberService;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.ui.Formats;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Screen to assign routines to members. On the left the user finds a member; on the
 * right it sees the member's current routine, can assign, complete or cancel it,
 * and reads the history. The rules live in {@link RoutineAssignmentService}.
 */
final class AssignmentPane extends HBox {

    private final RoutineAssignmentService assignments;
    private final RoutineService routines;

    private final MemberPicker picker;
    private final VBox detail = new VBox(16);
    private Member selected;   // socio elegido (null si no hay)

    /**
     * @param assignments the assignment service
     * @param routines    the routine service, to list and show routines
     * @param members     the member service, to search members
     */
    AssignmentPane(RoutineAssignmentService assignments, RoutineService routines, MemberService members) {
        super(24);
        this.assignments = assignments;
        this.routines = routines;
        this.picker = new MemberPicker("Asignación de rutinas", members, this::showMember);
        HBox.setHgrow(detail, Priority.ALWAYS);
        getChildren().addAll(picker, detail);
        render();
    }

    /** Reloads the member list and the detail of the selected member. */
    void refresh() {
        picker.reload();
        render();
    }

    private void showMember(Member member) {
        selected = member;
        render();
    }

    // Dibuja el panel derecho según el socio elegido.
    private void render() {
        detail.getChildren().clear();
        if (selected == null) {
            Label hint = new Label("Selecciona un socio para ver o asignar su rutina.");
            hint.getStyleClass().add("nf-muted");
            detail.getChildren().add(hint);
            return;
        }
        Label name = new Label(selected.getFullName());
        name.getStyleClass().add("nf-member-name");
        Label document = new Label("Documento " + selected.getDocumentNumber()
                + (selected.isActive() ? "" : "  ·  dado de baja"));
        document.getStyleClass().add("nf-muted");
        detail.getChildren().addAll(new VBox(2, name, document), buildCurrentCard(), buildHistory());
    }

    // Tarjeta con la rutina actual del socio y los botones según su estado.
    private Node buildCurrentCard() {
        Label title = new Label("Rutina actual");
        title.getStyleClass().add("nf-card-title");
        VBox card = new VBox(10, title);
        card.getStyleClass().add("nf-card");

        Optional<RoutineAssignment> current = assignments.findCurrentFor(selected.getId());
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        if (current.isPresent()) {
            RoutineAssignment assignment = current.get();
            Routine routine = routines.findById(assignment.getRoutineId());
            int dayCount = routine.getDays().size();
            Label routineName = new Label(routine.getName() + "  ·  " + dayCount
                    + (dayCount == 1 ? " día" : " días") + "  ·  " + routine.getTotalSets() + " series");
            routineName.getStyleClass().add("nf-list-name");
            Label since = new Label("Asignada el " + Formats.date(assignment.getAssignedAt())
                    + "  ·  " + routine.getObjective());
            since.getStyleClass().add("nf-muted");
            Button complete = new Button("Completar");
            complete.getStyleClass().add("nf-primary-small");
            complete.setOnAction(e -> close(assignment, true));
            Button cancel = new Button("Cancelar rutina");
            cancel.getStyleClass().add("nf-secondary");
            cancel.setOnAction(e -> close(assignment, false));
            buttons.getChildren().addAll(complete, cancel);
            card.getChildren().addAll(routineName, since, buttons);
        } else {
            Label none = new Label("Este socio no tiene una rutina activa.");
            none.getStyleClass().add("nf-muted");
            Button assign = new Button("Asignar rutina");
            assign.getStyleClass().add("nf-primary-small");
            assign.setOnAction(e -> assignRoutine());
            buttons.getChildren().add(assign);
            card.getChildren().addAll(none, buttons);
        }
        return card;
    }

    // Historial de asignaciones del socio, de la más reciente a la más antigua.
    private Node buildHistory() {
        Label title = new Label("Historial");
        title.getStyleClass().add("nf-section-title");

        TableView<RoutineAssignment> table = new TableView<>();
        TableColumn<RoutineAssignment, String> routine = FxTables.column("Rutina", 300, this::routineName);
        TableColumn<RoutineAssignment, String> date = FxTables.column("Fecha", 160,
                a -> Formats.date(a.getAssignedAt()));
        TableColumn<RoutineAssignment, String> status = FxTables.column("Estado", 160,
                a -> a.getStatus().getLabel());
        FxTables.colorize(status, text -> switch (text) {
            case "Activa" -> "nf-state-on";
            case "Cancelada" -> "nf-state-bad";
            default -> "nf-state-off";
        });
        table.getColumns().addAll(List.of(routine, date, status));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.setPlaceholder(new Label("Este socio aún no tiene rutinas asignadas."));
        table.getStyleClass().add("nf-table");
        List<RoutineAssignment> history = new ArrayList<>(assignments.findHistoryFor(selected.getId()));
        history.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        table.getItems().setAll(history);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox box = new VBox(10, title, table);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private String routineName(RoutineAssignment assignment) {
        try {
            return routines.findById(assignment.getRoutineId()).getName();
        } catch (NextFitException e) {
            return "-";
        }
    }

    // Pide elegir una rutina y la asigna; si una regla la rechaza, el mensaje sale en rojo dentro del formulario.
    private void assignRoutine() {
        List<Routine> available = routines.findAll();
        if (available.isEmpty()) {
            FxDialogs.message(this, "No hay rutinas", "Primero crea una rutina en la pestaña Rutinas.");
            return;
        }
        ComboBox<Routine> box = new ComboBox<>();
        box.getItems().setAll(available);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(Routine routine) {
                return routine == null ? "" : routine.getName();
            }

            @Override
            public Routine fromString(String text) {
                return null;
            }
        });
        box.getStyleClass().add("nf-input");
        box.setMaxWidth(Double.MAX_VALUE);
        box.getSelectionModel().selectFirst();
        VBox fields = new VBox(FxDialogs.labeled("Rutina para " + selected.getFullName(), box));
        fields.setPrefWidth(420);
        FxDialogs.form(this, "Asignar rutina", "Asignar", fields, () -> {
            if (box.getValue() == null) {
                throw new BusinessRuleException("Elige la rutina que se va a asignar.");
            }
            assignments.assign(selected.getId(), box.getValue().getId());
        });
        render();
    }

    // Completa o cancela la asignación actual, previa confirmación.
    private void close(RoutineAssignment assignment, boolean complete) {
        String text = complete ? "¿Marcar la rutina como completada?" : "¿Cancelar la rutina del socio?";
        if (FxDialogs.confirm(this, "Confirmar", text, complete ? "Completar" : "Cancelar rutina")) {
            try {
                if (complete) {
                    assignments.complete(assignment.getId());
                } else {
                    assignments.cancel(assignment.getId());
                }
            } catch (NextFitException e) {
                FxDialogs.error(this, e);
            }
            render();
        }
    }
}