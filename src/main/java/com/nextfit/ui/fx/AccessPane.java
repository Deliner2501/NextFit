package com.nextfit.ui.fx;

import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.AccessService;
import com.nextfit.service.AttendanceService;
import com.nextfit.service.MemberService;
import com.nextfit.ui.Formats;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Kiosk screen of the reception. The text box always keeps the focus, so a USB
 * barcode scanner (which types the code and presses Enter) works without
 * touching the mouse; the document number can also be typed by hand. The big
 * panel answers with green (entry), blue (exit) or red (denied) and the right
 * column lists who is inside now.
 */
final class AccessPane extends HBox {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-CO"));

    private final AccessService access;
    private final AttendanceService attendance;
    private final MemberService members;

    private final TextField scan = new TextField();
    private final VBox result = new VBox(14);
    private final ListView<AttendanceRecord> inside = new ListView<>();
    private final Label insideTitle = new Label();
    private final PauseTransition reset = new PauseTransition(Duration.seconds(7));

    AccessPane(AccessService access, AttendanceService attendance, MemberService members) {
        super(24);
        this.access = access;
        this.attendance = attendance;
        this.members = members;

        VBox left = new VBox(18, buildScanBox(), buildResultHolder());
        HBox.setHgrow(left, Priority.ALWAYS);
        getChildren().addAll(left, buildInsideColumn());
        showIdle();
        reset.setOnFinished(e -> showIdle());
    }

    /** Reloads the list of members inside and gives the focus back to the scan box. */
    void refresh() {
        reloadInside();
        Platform.runLater(scan::requestFocus);
    }

    private VBox buildScanBox() {
        Label title = new Label("Control de acceso");
        title.getStyleClass().add("nf-section-title");
        Label hint = new Label("Escanea el carnet o escribe el documento y presiona Enter. "
                + "La primera lectura registra la entrada; la siguiente, la salida.");
        hint.getStyleClass().add("nf-muted-small");
        hint.setWrapText(true);
        scan.setPromptText("Código del carnet o número de documento");
        scan.getStyleClass().add("nf-input");
        scan.setPrefHeight(52);
        scan.setOnAction(e -> submit());
        // The scanner types into the focused field: if the focus is lost, take it back.
        scan.focusedProperty().addListener((obs, was, now) -> {
            if (!now && scan.getScene() != null && scan.getScene().getWindow() != null
                    && scan.getScene().getWindow().isFocused() && isVisible()) {
                Platform.runLater(() -> {
                    if (scan.getScene() != null && scan.getScene().getFocusOwner() == null) {
                        scan.requestFocus();
                    }
                });
            }
        });
        return new VBox(8, title, hint, scan);
    }

    private StackPane buildResultHolder() {
        result.setAlignment(Pos.CENTER);
        result.setPadding(new Insets(32));
        result.getStyleClass().add("nf-access-card");
        StackPane holder = new StackPane(result);
        VBox.setVgrow(holder, Priority.ALWAYS);
        result.setMaxWidth(Double.MAX_VALUE);
        result.setMaxHeight(Double.MAX_VALUE);
        return holder;
    }

    private VBox buildInsideColumn() {
        insideTitle.getStyleClass().add("nf-section-title");
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
                Label since = new Label("Entró a las " + record.getCheckIn().format(TIME));
                since.getStyleClass().add("nf-muted-small");
                setText(null);
                setGraphic(new VBox(2, name, since));
            }
        });
        VBox.setVgrow(inside, Priority.ALWAYS);
        VBox column = new VBox(12, insideTitle, inside);
        column.setPrefWidth(340);
        column.setMinWidth(300);
        column.setMaxWidth(340);
        return column;
    }

    private void submit() {
        String code = scan.getText();
        scan.clear();
        AccessResult outcome = access.process(code);
        showResult(outcome);
        reloadInside();
        reset.playFromStart();
        scan.requestFocus();
    }

    private void showResult(AccessResult outcome) {
        result.getStyleClass().removeAll("nf-access-ok", "nf-access-exit", "nf-access-denied", "nf-access-idle");
        result.getChildren().clear();
        switch (outcome.outcome()) {
            case ENTRY -> result.getStyleClass().add("nf-access-ok");
            case EXIT -> result.getStyleClass().add("nf-access-exit");
            case DENIED -> result.getStyleClass().add("nf-access-denied");
        }
        Label verdict = new Label(switch (outcome.outcome()) {
            case ENTRY -> "ENTRADA REGISTRADA";
            case EXIT -> "SALIDA REGISTRADA";
            case DENIED -> "ACCESO DENEGADO";
        });
        verdict.getStyleClass().add("nf-access-verdict");

        if (outcome.member() != null) {
            result.getChildren().add(MemberAvatar.of(outcome.member(), 132));
            Label name = new Label(outcome.member().getFullName());
            name.getStyleClass().add("nf-access-name");
            result.getChildren().addAll(verdict, name);
        } else {
            result.getChildren().add(verdict);
        }
        Label message = new Label(outcome.message());
        message.getStyleClass().add("nf-access-message");
        message.setWrapText(true);
        message.setMaxWidth(520);
        message.setAlignment(Pos.CENTER);
        result.getChildren().add(message);

        if (outcome.granted() && outcome.validUntil() != null) {
            Label validity = new Label(validityText(outcome));
            validity.getStyleClass().add("nf-access-message");
            result.getChildren().add(validity);
        }
    }

    private static String validityText(AccessResult outcome) {
        String until = "Membresía hasta el " + outcome.validUntil().format(DAY);
        if (outcome.daysLeft() == 0) {
            return until + " · vence hoy";
        }
        if (outcome.daysLeft() > 0 && outcome.daysLeft() <= 7) {
            return until + " · quedan " + Formats.days((int) outcome.daysLeft());
        }
        return until;
    }

    private void showIdle() {
        result.getStyleClass().removeAll("nf-access-ok", "nf-access-exit", "nf-access-denied", "nf-access-idle");
        result.getStyleClass().add("nf-access-idle");
        result.getChildren().clear();
        result.getChildren().add(FxIcons.of(FxIcons.ACCESS, 72));
        Label waiting = new Label("Listo para escanear");
        waiting.getStyleClass().add("nf-access-verdict");
        Label help = new Label("Acerca el código QR del carnet al lector");
        help.getStyleClass().add("nf-access-message");
        result.getChildren().addAll(waiting, help);
    }

    private void reloadInside() {
        var current = attendance.findInside();
        inside.getItems().setAll(current);
        insideTitle.setText("Dentro ahora (" + current.size() + ")");
    }
}
