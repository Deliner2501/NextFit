package com.nextfit.ui.fx;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.Member;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineAssignment;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.TrainingSession;
import com.nextfit.service.MemberService;
import com.nextfit.service.RoutineAssignmentService;
import com.nextfit.service.RoutineService;
import com.nextfit.service.TrainingSessionService;
import com.nextfit.ui.Formats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Screen to log training sessions. The user finds a member; if the member has an
 * active routine, a form lets them record a session (day, date, minutes, notes) and
 * the history of sessions is listed below. The rules live in {@link TrainingSessionService}.
 */
final class SessionsPane extends HBox {

    private final TrainingSessionService sessions;
    private final RoutineAssignmentService assignments;
    private final RoutineService routines;

    private final MemberPicker picker;
    private final VBox detail = new VBox(16);
    private Member selected;   // socio elegido (null si no hay)

    /**
     * @param sessions    the session service
     * @param assignments the assignment service, to know the member's current routine
     * @param routines    the routine service, to list the days of that routine
     * @param members     the member service, to search members
     */
    SessionsPane(TrainingSessionService sessions, RoutineAssignmentService assignments,
                 RoutineService routines, MemberService members) {
        super(24);
        this.sessions = sessions;
        this.assignments = assignments;
        this.routines = routines;
        this.picker = new MemberPicker("Sesiones de entrenamiento", members, this::showMember);
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
            Label hint = new Label("Selecciona un socio para registrar o ver sus sesiones.");
            hint.getStyleClass().add("nf-muted");
            detail.getChildren().add(hint);
            return;
        }
        Label name = new Label(selected.getFullName());
        name.getStyleClass().add("nf-member-name");
        Label document = new Label("Documento " + selected.getDocumentNumber()
                + (selected.isActive() ? "" : "  ·  dado de baja"));
        document.getStyleClass().add("nf-muted");
        detail.getChildren().addAll(new VBox(2, name, document), buildFormCard(), buildHistory());
    }

    // Tarjeta para registrar una sesión, o un aviso si el socio no tiene rutina activa.
    private Node buildFormCard() {
        Label title = new Label("Registrar entrenamiento");
        title.getStyleClass().add("nf-card-title");
        VBox card = new VBox(12, title);
        card.getStyleClass().add("nf-card");
        card.setPadding(new Insets(20));

        Optional<RoutineAssignment> current = assignments.findCurrentFor(selected.getId());
        if (current.isEmpty()) {
            Label none = new Label("Este socio no tiene una rutina activa. Asígnale una en la pestaña Asignación.");
            none.getStyleClass().add("nf-muted");
            card.getChildren().add(none);
        } else {
            card.getChildren().addAll(buildForm(current.get()));
        }
        return card;
    }

    // Campos del formulario: día de la rutina, fecha, minutos, notas y botón.
    private List<Node> buildForm(RoutineAssignment assignment) {
        Routine routine = routines.findById(assignment.getRoutineId());
        Label routineLabel = new Label("Rutina: " + routine.getName());
        routineLabel.getStyleClass().add("nf-list-name");

        ComboBox<String> dayBox = new ComboBox<>();
        for (RoutineDay day : routine.getDays()) {
            dayBox.getItems().add(day.getName());
        }
        dayBox.getSelectionModel().selectFirst();
        dayBox.getStyleClass().add("nf-input");
        dayBox.setMaxWidth(Double.MAX_VALUE);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("nf-input");
        datePicker.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> minutes = new Spinner<>(5, 300, 45, 5);
        minutes.setEditable(true);
        minutes.getStyleClass().add("nf-input");
        minutes.setMaxWidth(Double.MAX_VALUE);
        commitOnFocusLost(minutes);

        TextField notes = new TextField();
        notes.setPromptText("Notas (opcional)");
        notes.getStyleClass().add("nf-input");

        Button register = new Button("Registrar sesión");
        register.getStyleClass().add("nf-primary-small");
        register.setOnAction(e -> register(assignment, dayBox.getValue(), datePicker.getValue(),
                minutes.getValue(), notes.getText().trim()));

        HBox row = new HBox(12,
                FxDialogs.labeled("Día", dayBox),
                FxDialogs.labeled("Fecha", datePicker),
                FxDialogs.labeled("Minutos", minutes));
        HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
        HBox notesRow = new HBox(12, FxDialogs.labeled("Notas", notes), register);
        notesRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(notesRow.getChildren().get(0), Priority.ALWAYS);
        return List.of(routineLabel, row, notesRow);
    }

    // Si el usuario escribe los minutos y pasa a otro campo sin dar Enter, igual se guardan.
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

    // Arma la sesión y la registra con el servicio; si una regla la rechaza, se muestra el aviso.
    private void register(RoutineAssignment assignment, String dayName, LocalDate date,
                          int minutes, String notes) {
        try {
            if (date == null) {
                throw new InvalidDataException("fecha", "elige la fecha de la sesión");
            }
            sessions.log(new TrainingSession(0, selected.getId(), assignment.getId(),
                    dayName == null ? "" : dayName, date, minutes, notes));
            render();
        } catch (NextFitException e) {
            FxDialogs.error(this, e);
        }
    }

    // Historial de sesiones del socio, de la más reciente a la más antigua.
    private Node buildHistory() {
        List<TrainingSession> history = new ArrayList<>(sessions.findByMember(selected.getId()));
        history.sort(Comparator.comparing(TrainingSession::getDate).thenComparing(TrainingSession::getId)
                .reversed());

        Label title = new Label("Historial de sesiones");
        title.getStyleClass().add("nf-section-title");
        Label count = new Label(history.size() == 1 ? "1 sesión" : history.size() + " sesiones");
        count.getStyleClass().add("nf-muted");

        TableView<TrainingSession> table = new TableView<>();
        TableColumn<TrainingSession, String> date = FxTables.column("Fecha", 150,
                s -> Formats.date(s.getDate()));
        TableColumn<TrainingSession, String> day = FxTables.column("Día", 240, TrainingSession::getDayName);
        TableColumn<TrainingSession, String> duration = FxTables.column("Duración", 130,
                s -> s.getDurationMinutes() + " min");
        TableColumn<TrainingSession, String> notes = FxTables.column("Notas", 300, TrainingSession::getNotes);
        table.getColumns().addAll(List.of(date, day, duration, notes));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(46);
        table.setPlaceholder(new Label("Este socio aún no tiene sesiones registradas."));
        table.getStyleClass().add("nf-table");
        table.getItems().setAll(history);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox box = new VBox(10, new VBox(2, title, count), table);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }
}