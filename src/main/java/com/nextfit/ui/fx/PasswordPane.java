package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.MemberPasswordService;
import com.nextfit.service.MemberService;
import com.nextfit.service.SelfAccessService;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The "Contraseña" screen: a member enters with the document and the password he created, and the staff
 * helps to create or reset a password. The member types the password himself; the staff never sees it.
 */
final class PasswordPane extends HBox {

    private final SelfAccessService selfAccess;
    private final MemberPasswordService passwords;
    private final Supplier<String> actor;

    private final TextField documentField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label outcome = new Label();
    private final Label status = new Label("Elige un socio");
    private final Button createButton = new Button("Crear contraseña");
    private final Button resetButton = new Button("Restablecer contraseña");
    private final PauseTransition clearOutcome = new PauseTransition(Duration.seconds(6));
    private final MemberPicker picker;

    PasswordPane(SelfAccessService selfAccess, MemberPasswordService passwords, MemberService members,
                 Supplier<String> actor, Function<Member, List<String>> tags) {
        super(24);
        this.selfAccess = selfAccess;
        this.passwords = passwords;
        this.actor = actor;
        this.picker = new MemberPicker("Contraseña de un socio", members, m -> updateStatus(), tags);
        setPadding(new Insets(0, 4, 4, 4));
        clearOutcome.setOnFinished(e -> outcome.setText(""));
        getChildren().addAll(buildEntryColumn(), buildStaffColumn());
        sceneProperty().addListener((obs, old, now) -> {
            if (now == null) {
                clearFields();
                say("");
            }
        });
    }

    /** Called when the screen is shown again. */
    void refresh() {
        clearFields();
        say("");
        picker.reload();
        updateStatus();
    }

    private VBox buildEntryColumn() {
        Label title = new Label("Entrada y salida con contraseña");
        title.getStyleClass().add("nf-section-title");
        Label help = new Label("El socio escribe su documento y la contraseña que creó. El sistema registra su entrada, o su salida si ya estaba dentro.");
        help.getStyleClass().add("nf-muted-small");
        help.setWrapText(true);

        documentField.setPromptText("Número de documento");
        passwordField.setPromptText("Contraseña");
        passwordField.setOnAction(e -> enter());
        documentField.setOnAction(e -> passwordField.requestFocus());

        outcome.getStyleClass().add("nf-access-verdict");
        outcome.setWrapText(true);
        Button enter = new Button("Registrar acceso");
        enter.getStyleClass().add("nf-primary");
        enter.setOnAction(e -> enter());
        Label note = new Label("¿Primera vez o la olvidaste? Pide ayuda en recepción para crearla de nuevo.");
        note.getStyleClass().add("nf-muted-small");
        note.setWrapText(true);

        VBox box = new VBox(12, title, help, FxDialogs.labeled("Documento", documentField),
                FxDialogs.labeled("Contraseña", passwordField), enter, outcome, note);
        box.setAlignment(Pos.TOP_LEFT);
        box.setMaxWidth(520);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildStaffColumn() {
        status.getStyleClass().add("nf-muted-small");
        status.setWrapText(true);
        createButton.getStyleClass().add("nf-primary");
        createButton.setOnAction(e -> create());
        resetButton.getStyleClass().add("nf-secondary");
        resetButton.setOnAction(e -> reset());
        HBox buttons = new HBox(10, createButton, resetButton);
        VBox box = new VBox(12, picker, status, buttons);
        box.setPrefWidth(380);
        box.setMinWidth(340);
        VBox.setVgrow(picker, Priority.ALWAYS);
        return box;
    }

    private void enter() {
        AccessResult result = selfAccess.enter(documentField.getText(), passwordField.getText());
        passwordField.clear();
        if (result.outcome() == AccessResult.Outcome.DENIED) {
            say("DENEGADO: " + result.message());
            return;
        }
        documentField.clear();
        say((result.outcome() == AccessResult.Outcome.ENTRY ? "ENTRADA: " : "SALIDA: ")
                + result.member().getFullName());
    }

    private void create() {
        Member member = picker.selected();
        if (member == null) {
            return;
        }
        PasswordField first = new PasswordField();
        first.setPromptText("Al menos " + MemberPasswordService.MIN_LENGTH + " caracteres");
        PasswordField second = new PasswordField();
        second.setPromptText("Repítela");
        Label help = new Label("El socio escribe su contraseña. Quien ayuda no debe mirarla.");
        help.getStyleClass().add("nf-muted-small");
        help.setWrapText(true);
        VBox fields = new VBox(12, help, FxDialogs.labeled("Contraseña de " + member.getFullName(), first),
                FxDialogs.labeled("Repetir contraseña", second));
        FxDialogs.form(this, "Crear contraseña", "Guardar", fields, () -> {
            passwords.create(member, first.getText(), second.getText(), actor.get());
            say("Contraseña creada para " + member.getFullName());
            updateStatus();
        });
    }

    private void reset() {
        Member member = picker.selected();
        if (member == null || !FxDialogs.confirm(this, "Restablecer contraseña",
                "Se borrará la contraseña de " + member.getFullName() + ". Después podrá crear una nueva.",
                "Restablecer")) {
            return;
        }
        passwords.reset(member, actor.get());
        say("Contraseña restablecida: " + member.getFullName());
        updateStatus();
    }

    private void updateStatus() {
        if (picker == null) {
            return;
        }
        picker.refreshTags();
        Member member = picker.selected();
        boolean has = member != null && passwords.hasPassword(member.getId());
        status.setText(member == null ? "Elige un socio"
                : has ? member.getFullName() + " ya tiene contraseña."
                : member.getFullName() + " todavía no tiene contraseña.");
        createButton.setDisable(member == null || has);
        resetButton.setDisable(!has);
    }

    private void clearFields() {
        documentField.clear();
        passwordField.clear();
    }

    /** Shows a result and removes it after a few seconds. */
    private void say(String text) {
        outcome.setText(text);
        clearOutcome.stop();
        if (!text.isEmpty()) {
            clearOutcome.playFromStart();
        }
    }
}
