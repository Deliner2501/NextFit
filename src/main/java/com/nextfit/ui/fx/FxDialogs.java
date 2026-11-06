package com.nextfit.ui.fx;

import com.nextfit.exceptions.NextFitException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modal windows shared by every module: a form that stays open and shows the
 * error when the save fails, and a simple message. They use the same style
 * sheet and the active theme as the main window.
 */
public final class FxDialogs {

    /** An action that may fail with a business error. */
    @FunctionalInterface
    public interface Action {
        /**
         * Runs the action.
         *
         * @throws NextFitException when a rule is broken; the message is shown to the user
         */
        void run() throws NextFitException;
    }

    private static FxTheme theme;

    private FxDialogs() {
    }

    /**
     * Sets the theme used by the dialogs; called once by {@link FxApp}.
     *
     * @param activeTheme the theme of the application
     */
    static void useTheme(FxTheme activeTheme) {
        theme = activeTheme;
    }

    /**
     * Shows a form. The window closes only when the save action succeeds; if it
     * throws, the message appears in red inside the form.
     *
     * @param owner    a node of the window the dialog belongs to
     * @param title    the title of the dialog
     * @param saveText the text of the confirm button
     * @param fields   the form fields
     * @param save     the action that saves; may throw a {@link NextFitException}
     */
    public static void form(Node owner, String title, String saveText, Node fields, Action save) {
        Label heading = new Label(title);
        heading.getStyleClass().add("nf-dialog-title");
        Label error = new Label();
        error.getStyleClass().add("nf-error");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        Stage stage = newStage(owner, title);
        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().add("nf-secondary");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> stage.close());
        Button confirm = new Button(saveText);
        confirm.getStyleClass().add("nf-primary");
        confirm.setDefaultButton(true);
        confirm.setOnAction(e -> {
            try {
                save.run();
                stage.close();
            } catch (NextFitException ex) {
                error.setText(ex.getMessage());
                error.setManaged(true);
                error.setVisible(true);
                stage.sizeToScene();
            }
        });
        HBox buttons = new HBox(10, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        show(stage, new VBox(16, heading, fields, error, buttons));
    }

    /**
     * Shows a message with one button.
     *
     * @param owner   a node of the window the dialog belongs to
     * @param title   the title
     * @param message the text
     */
    public static void message(Node owner, String title, String message) {
        Stage stage = newStage(owner, title);
        Label heading = new Label(title);
        heading.getStyleClass().add("nf-dialog-title");
        Label text = new Label(message);
        text.getStyleClass().add("nf-muted");
        text.setWrapText(true);
        Button ok = new Button("Aceptar");
        ok.getStyleClass().add("nf-primary");
        ok.setDefaultButton(true);
        ok.setCancelButton(true);
        ok.setOnAction(e -> stage.close());
        HBox buttons = new HBox(ok);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        show(stage, new VBox(16, heading, text, buttons));
    }

    /**
     * Asks the user to confirm an action and waits for the answer.
     *
     * @param owner       a node of the window the dialog belongs to
     * @param title       the title
     * @param message     the question
     * @param confirmText the text of the confirm button
     * @return true when the user confirmed
     */
    public static boolean confirm(Node owner, String title, String message, String confirmText) {
        boolean[] accepted = {false};
        Stage stage = newStage(owner, title);
        Label heading = new Label(title);
        heading.getStyleClass().add("nf-dialog-title");
        Label text = new Label(message);
        text.getStyleClass().add("nf-muted");
        text.setWrapText(true);
        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().add("nf-secondary");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> stage.close());
        Button confirm = new Button(confirmText);
        confirm.getStyleClass().add("nf-primary");
        confirm.setDefaultButton(true);
        confirm.setOnAction(e -> {
            accepted[0] = true;
            stage.close();
        });
        HBox buttons = new HBox(10, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        show(stage, new VBox(16, heading, text, buttons));
        return accepted[0];
    }

    /**
     * Shows a read-only text, such as a receipt, with a button to copy it.
     *
     * @param owner   a node of the window the dialog belongs to
     * @param title   the window title
     * @param heading the heading inside the dialog
     * @param text    the text to show, in a monospaced font
     */
    public static void document(Node owner, String title, String heading, String text) {
        Stage stage = newStage(owner, title);
        Label head = new Label(heading);
        head.getStyleClass().add("nf-success-title");
        TextArea body = new TextArea(text);
        body.setEditable(false);
        body.setWrapText(false);
        body.setPrefColumnCount(44);
        body.setPrefRowCount(Math.min(26, text.split("\n").length + 3));
        body.getStyleClass().add("nf-receipt");
        Button copy = new Button("Copiar");
        copy.getStyleClass().add("nf-secondary");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            copy.setText("Copiado");
        });
        Button close = new Button("Cerrar");
        close.getStyleClass().add("nf-primary");
        close.setDefaultButton(true);
        close.setCancelButton(true);
        close.setOnAction(e -> stage.close());
        HBox buttons = new HBox(10, copy, close);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        show(stage, new VBox(16, head, body, buttons));
    }

    /**
     * Shows the message of a business error.
     *
     * @param owner a node of the window the dialog belongs to
     * @param error the error
     */
    public static void error(Node owner, NextFitException error) {
        message(owner, "No se pudo completar", error.getMessage());
    }

    private static Stage newStage(Node owner, String title) {
        Stage stage = new Stage();
        Window window = owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
        if (window != null) {
            stage.initOwner(window);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setResizable(false);
        return stage;
    }

    private static void show(Stage stage, VBox content) {
        content.getStyleClass().addAll("nf-bg", "nf-dialog");
        content.setPadding(new Insets(28));
        content.setMinWidth(440);
        Scene scene = new Scene(content);
        if (theme != null) {
            scene.getStylesheets().add(FxDialogs.class.getResource("/fx/nextfit.css").toExternalForm());
            scene.getStylesheets().add(FxDialogs.class.getResource("/fx/nextfit-violet.css").toExternalForm());
            theme.mark(content);
        }
        stage.setScene(scene);
        stage.showAndWait();
    }

    /**
     * Creates a labeled field row for forms.
     *
     * @param label the text above the field
     * @param field the control
     * @return the label and the field, stacked
     */
    public static VBox labeled(String label, Node field) {
        Label text = new Label(label);
        text.getStyleClass().add("nf-field-label");
        VBox box = new VBox(6, text, field);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        VBox.setVgrow(field, Priority.NEVER);
        return box;
    }
}
