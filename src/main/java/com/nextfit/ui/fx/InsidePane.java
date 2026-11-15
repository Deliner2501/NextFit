package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Screen of the reception that lists who is inside the gym now. Members enter and leave by face or
 * by document and password; the only action here is for the staff to close the visit of a member
 * who forgot to mark the exit.
 */
final class InsidePane extends VBox {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d/MM", Locale.forLanguageTag("es-CO"));

    private final AttendanceService attendance;
    private final MemberService members;
    private final ListView<AttendanceRecord> inside = new ListView<>();
    private final Label title = new Label();
    private final Button forceExit = new Button("Registrar salida");

    InsidePane(AttendanceService attendance, MemberService members) {
        super(12);
        this.attendance = attendance;
        this.members = members;

        title.getStyleClass().add("nf-section-title");
        Label hint = new Label("Los socios entran y salen con su rostro o con su documento y contraseña. "
                + "Usa «Registrar salida» solo cuando a un socio se le olvidó marcar su salida: "
                + "elige a la persona en la lista y presiona el botón.");
        hint.getStyleClass().add("nf-muted-small");
        hint.setWrapText(true);

        inside.getStyleClass().add("nf-list");
        inside.setPlaceholder(new Label("Nadie ha entrado todavía"));
        inside.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(AttendanceRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (empty || record == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Member member = members.findById(record.getMemberId());
                Label name = new Label(member.getFullName());
                name.getStyleClass().add("nf-list-name");
                String day = record.getCheckIn().toLocalDate().equals(java.time.LocalDate.now())
                        ? "" : record.getCheckIn().format(DAY) + " ";
                Label since = new Label("Entró " + (day.isEmpty() ? "a las " : "el " + day + "a las ")
                        + record.getCheckIn().format(TIME));
                since.getStyleClass().add("nf-muted-small");
                setText(null);
                setGraphic(new VBox(2, name, since));
            }
        });
        inside.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> forceExit.setDisable(now == null));
        VBox.setVgrow(inside, Priority.ALWAYS);

        forceExit.getStyleClass().add("nf-secondary");
        forceExit.setDisable(true);
        forceExit.setOnAction(e -> closeVisit());

        setPadding(new Insets(0, 0, 8, 0));
        // The button goes above the list, so it is always visible however long the list is.
        getChildren().addAll(title, hint, forceExit, inside);
    }

    /** Reloads the list of members inside. */
    void refresh() {
        var current = attendance.findInside();
        inside.getItems().setAll(current);
        title.setText("Dentro ahora (" + current.size() + ")");
        forceExit.setDisable(true);
    }

    private void closeVisit() {
        AttendanceRecord chosen = inside.getSelectionModel().getSelectedItem();
        if (chosen == null) {
            return;
        }
        Member member = members.findById(chosen.getMemberId());
        boolean sure = FxDialogs.confirm(forceExit, "Registrar salida",
                "¿Registrar la salida de " + member.getFullName() + "? Úsalo solo si olvidó marcarla.",
                "Registrar salida");
        if (!sure) {
            return;
        }
        try {
            attendance.checkOut(member.getId());
        } catch (NextFitException e) {
            FxDialogs.error(forceExit, e);
        }
        refresh();
    }
}