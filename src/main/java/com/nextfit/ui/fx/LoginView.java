package com.nextfit.ui.fx;

import com.nextfit.model.User;
import com.nextfit.service.AuthService;
import com.nextfit.service.Session;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The sign-in screen. It only collects the credentials and shows the result;
 * checking them is the job of {@link AuthService}.
 */
final class LoginView extends HBox {

    private final AuthService auth;
    private final Session session;
    private final Runnable onSuccess;
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Ingresar");
    private final Label errorLabel = new Label(" ");

    /**
     * @param auth      the sign-in rules
     * @param session   where the signed-in user is stored
     * @param onSuccess action to run after a successful sign-in
     */
    LoginView(AuthService auth, Session session, Runnable onSuccess) {
        this.auth = auth;
        this.session = session;
        this.onSuccess = onSuccess;
        getStyleClass().add("nf-bg");
        Region brand = buildBrand();
        brand.prefWidthProperty().bind(widthProperty().multiply(0.40));
        brand.setMinWidth(380);
        StackPane form = new StackPane(buildForm());
        HBox.setHgrow(form, Priority.ALWAYS);
        getChildren().addAll(brand, form);
    }

    private Region buildBrand() {
        Region photo = FxImages.cover("/images/login/background.jpg", 0.5, 0.35);
        Region shade = new Region();
        shade.getStyleClass().add("nf-brand-shade");

        VBox text = new VBox(10);
        text.setAlignment(Pos.BOTTOM_LEFT);
        text.setPadding(new Insets(56));
        Image logo = FxImages.load("/images/logo/nextfit-logo-512.png");
        if (logo != null) {
            ImageView mark = new ImageView(logo);
            mark.setFitWidth(80);
            mark.setPreserveRatio(true);
            mark.setSmooth(true);
            text.getChildren().add(mark);
        }
        Label name = new Label("NextFit");
        name.getStyleClass().add("nf-brand-name");
        Label slogan = new Label("Entrena. Mide. Mejora.");
        slogan.getStyleClass().add("nf-brand-slogan");
        Label description = new Label("Gestión de socios, membresías, salud\ny rutinas en un solo lugar.");
        description.getStyleClass().add("nf-brand-text");
        Label credit = new Label("Programación III · Universidad Popular del Cesar");
        credit.getStyleClass().add("nf-brand-credit");
        VBox.setMargin(credit, new Insets(40, 0, 0, 0));
        text.getChildren().addAll(name, slogan, description, credit);

        StackPane brand = new StackPane(photo, shade, text);
        brand.setAlignment(Pos.BOTTOM_LEFT);
        return brand;
    }

    private VBox buildForm() {
        Label title = new Label("Bienvenido");
        title.getStyleClass().add("nf-title");
        Label subtitle = new Label("Ingresa con tu cuenta para gestionar el gimnasio");
        subtitle.getStyleClass().add("nf-muted");

        Label userLabel = new Label("Usuario");
        userLabel.getStyleClass().add("nf-field-label");
        usernameField.setPromptText("Tu nombre de usuario");
        Label passwordLabel = new Label("Contraseña");
        passwordLabel.getStyleClass().add("nf-field-label");
        passwordField.setPromptText("Tu contraseña");
        for (TextField field : new TextField[] {usernameField, passwordField}) {
            field.getStyleClass().add("nf-input");
            field.setPrefHeight(48);
            field.setOnAction(e -> attemptLogin());
        }

        errorLabel.getStyleClass().add("nf-error");
        errorLabel.setWrapText(true);
        loginButton.getStyleClass().add("nf-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(52);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> attemptLogin());

        VBox form = new VBox(8, title, subtitle, spacer(18), userLabel, usernameField,
                spacer(6), passwordLabel, passwordField, errorLabel, loginButton);
        form.setMaxWidth(420);
        form.setAlignment(Pos.CENTER_LEFT);
        return form;
    }

    private static Region spacer(double height) {
        Region space = new Region();
        space.setMinHeight(height);
        space.setPrefHeight(height);
        return space;
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        setBusy(true);
        errorLabel.setText(" ");
        Task<User> task = new Task<>() {
            @Override
            protected User call() {
                return auth.login(username, password);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            session.start(task.getValue());
            onSuccess.run();
        });
        task.setOnFailed(e -> {
            setBusy(false);
            Throwable cause = task.getException();
            errorLabel.setText(cause == null || cause.getMessage() == null
                    ? "No se pudo iniciar sesión" : cause.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        });
        Thread worker = new Thread(task, "login");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Verificando…" : "Ingresar");
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
    }

    /** Puts the cursor in the first field. */
    void focusUsername() {
        usernameField.requestFocus();
    }
}
