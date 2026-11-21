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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * The sign-in screen: the brand and what the system does on the left, the sign-in card on the right,
 * over a photo of a gym. It only collects the credentials and shows the result; checking them is
 * the job of {@link AuthService}. The screen keeps its own dark colors in both themes, because it
 * is drawn over a photo.
 */
final class LoginView extends StackPane {

    private static final String LOGIN_LABEL = "Ingresar  →";

    private final AuthService auth;
    private final Session session;
    private final Runnable onSuccess;
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField visiblePasswordField = new TextField();
    private final Button revealButton = new Button();
    private final Button loginButton = new Button(LOGIN_LABEL);
    private final Label errorLabel = new Label();

    /**
     * @param auth      the sign-in rules
     * @param session   where the signed-in user is stored
     * @param onSuccess action to run after a successful sign-in
     */
    LoginView(AuthService auth, Session session, Runnable onSuccess) {
        this.auth = auth;
        this.session = session;
        this.onSuccess = onSuccess;
        getStyleClass().addAll("nf-bg", "nf-login");

        Region photo = FxImages.cover("/images/login/background-gym.jpg", 0.5, 0.5);
        Region shade = new Region();
        shade.getStyleClass().add("nf-login-shade");

        VBox brand = buildBrand();
        HBox.setHgrow(brand, Priority.ALWAYS);
        StackPane card = new StackPane(buildCard());
        card.setPadding(new Insets(28, 48, 28, 16));
        card.setMinWidth(500);
        card.setPrefWidth(540);
        HBox content = new HBox(brand, card);
        content.setAlignment(Pos.CENTER);
        getChildren().addAll(photo, shade, content);
    }

    /** The left side: name, headline, what the system does and the credit. */
    private VBox buildBrand() {
        HBox name = new HBox(14);
        name.setAlignment(Pos.CENTER_LEFT);
        Image logo = FxImages.load("/images/logo/nextfit-logo-512.png");
        if (logo != null) {
            ImageView mark = new ImageView(logo);
            mark.setFitWidth(58);
            mark.setPreserveRatio(true);
            mark.setSmooth(true);
            name.getChildren().add(mark);
        }
        Label brandName = new Label("NextFit");
        brandName.getStyleClass().add("nf-login-brand");
        name.getChildren().add(brandName);

        Region bar = new Region();
        bar.getStyleClass().add("nf-brand-bar");
        bar.setMinSize(64, 6);
        bar.setPrefSize(64, 6);
        bar.setMaxSize(64, 6);

        TextFlow headline = new TextFlow(
                styled("Gestiona tu ", "nf-login-headline"),
                styled("gimnasio", "nf-login-headline-accent"),
                styled(" de forma inteligente", "nf-login-headline"));
        headline.setMaxWidth(560);
        Label description = new Label("Controla socios, membresías, accesos, entrenamientos y más, en una sola plataforma.");
        description.getStyleClass().add("nf-login-text");
        description.setWrapText(true);
        description.setMaxWidth(520);

        GridPane features = new GridPane();
        features.setHgap(14);
        features.setVgap(14);
        String[][] items = {
            {FxIcons.MEMBERS, "Gestión de socios"},
            {FxIcons.BILLING, "Membresías y pagos"},
            {FxIcons.ACCESS, "Control de acceso"},
            {FxIcons.TRAINING, "Planes de entrenamiento"},
            {FxIcons.ANALYTICS, "Reportes y analítica"},
            {FxIcons.BELL, "Alertas y notificaciones"},
        };
        for (int i = 0; i < items.length; i++) {
            features.add(feature(items[i][0], items[i][1]), i % 3, i / 3);
        }

        Label credit = new Label("Programación de computadores III · Universidad Popular del Cesar");
        credit.getStyleClass().add("nf-brand-credit");

        VBox brand = new VBox(18, name, bar, headline, description, features, credit);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(36, 24, 36, 64));
        brand.setMinWidth(530);
        return brand;
    }

    private static Text styled(String text, String styleClass) {
        Text part = new Text(text);
        part.getStyleClass().add(styleClass);
        return part;
    }

    private static VBox feature(String icon, String text) {
        Region symbol = FxIcons.of(icon, 28);
        Label label = new Label(text);
        label.getStyleClass().add("nf-login-feature-text");
        label.setWrapText(true);
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(116);
        VBox tile = new VBox(8, symbol, label);
        tile.getStyleClass().add("nf-login-feature");
        tile.setAlignment(Pos.CENTER);
        tile.setMinSize(136, 100);
        tile.setPrefSize(136, 100);
        return tile;
    }

    /** The right side: the card with the credentials. */
    private VBox buildCard() {
        HBox name = new HBox(12);
        name.setAlignment(Pos.CENTER);
        Image logo = FxImages.load("/images/logo/nextfit-logo-512.png");
        if (logo != null) {
            ImageView mark = new ImageView(logo);
            mark.setFitWidth(54);
            mark.setPreserveRatio(true);
            mark.setSmooth(true);
            name.getChildren().add(mark);
        }
        Label brandName = new Label("NextFit");
        brandName.getStyleClass().add("nf-login-card-brand");
        name.getChildren().add(brandName);

        TextFlow title = new TextFlow(styled("Iniciar ", "nf-login-title"), styled("sesión", "nf-login-title-accent"));
        Label subtitle = new Label("Ingresa con tu cuenta para gestionar el gimnasio");
        subtitle.getStyleClass().add("nf-login-subtitle");
        subtitle.setWrapText(true);

        usernameField.setPromptText("Tu nombre de usuario");
        passwordField.setPromptText("Tu contraseña");
        visiblePasswordField.setPromptText("Tu contraseña");
        // The two password fields share the text; only one of them is shown.
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        for (TextField field : new TextField[] {usernameField, passwordField, visiblePasswordField}) {
            field.getStyleClass().add("nf-input");
            field.setPrefHeight(50);
            field.setOnAction(e -> attemptLogin());
        }
        // Room at the right for the eye button.
        passwordField.getStyleClass().add("nf-input-reveal");
        visiblePasswordField.getStyleClass().add("nf-input-reveal");

        revealButton.getStyleClass().add("nf-eye");
        revealButton.setGraphic(FxIcons.of(FxIcons.EYE, 20));
        revealButton.setFocusTraversable(false);
        revealButton.setOnAction(e -> toggleReveal());
        StackPane passwordBox = new StackPane(passwordField, visiblePasswordField, revealButton);
        StackPane.setAlignment(revealButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(revealButton, new Insets(0, 6, 0, 0));

        errorLabel.getStyleClass().addAll("nf-error", "nf-login-error");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
        loginButton.getStyleClass().add("nf-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(54);
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(e -> attemptLogin());

        Label noAccount = new Label("¿Aún no tienes una cuenta?");
        noAccount.getStyleClass().add("nf-login-subtitle");
        Label askAdmin = new Label("Pide al administrador que cree tu usuario.");
        askAdmin.getStyleClass().add("nf-login-subtitle");
        VBox help = new VBox(2, noAccount, askAdmin);
        help.setAlignment(Pos.CENTER);
        help.getStyleClass().add("nf-login-help");

        VBox form = new VBox(8, name, spacer(6), title, subtitle, spacer(12),
                fieldLabel("Usuario", FxIcons.PERSON), usernameField,
                spacer(4), fieldLabel("Contraseña", FxIcons.LOCK), passwordBox,
                errorLabel, loginButton, spacer(8), help);
        form.getStyleClass().add("nf-login-card");
        form.setMaxWidth(460);
        form.setMaxHeight(Region.USE_PREF_SIZE);
        form.setAlignment(Pos.CENTER_LEFT);
        return form;
    }

    private static HBox fieldLabel(String text, String icon) {
        Region symbol = FxIcons.of(icon, 16);
        Label label = new Label(text);
        label.getStyleClass().add("nf-login-field-label");
        HBox row = new HBox(8, symbol, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Region spacer(double height) {
        Region space = new Region();
        space.setMinHeight(height);
        space.setPrefHeight(height);
        return space;
    }

    /** Shows or hides the password the member is typing. */
    private void toggleReveal() {
        boolean show = !visiblePasswordField.isVisible();
        visiblePasswordField.setVisible(show);
        visiblePasswordField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
        revealButton.setGraphic(FxIcons.of(show ? FxIcons.EYE_OFF : FxIcons.EYE, 20));
        (show ? visiblePasswordField : passwordField).requestFocus();
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        setBusy(true);
        errorLabel.setText("");
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
            (visiblePasswordField.isVisible() ? visiblePasswordField : passwordField).requestFocus();
        });
        Thread worker = new Thread(task, "login");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        loginButton.setText(busy ? "Verificando…" : LOGIN_LABEL);
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        visiblePasswordField.setDisable(busy);
        revealButton.setDisable(busy);
    }

    /** Puts the cursor in the first field. */
    void focusUsername() {
        usernameField.requestFocus();
    }
}
