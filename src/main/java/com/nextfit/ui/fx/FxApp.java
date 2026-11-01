package com.nextfit.ui.fx;

import com.nextfit.service.AuthService;
import com.nextfit.service.DashboardService;
import com.nextfit.service.Session;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Entry point of the JavaFX interface. It owns the only window and swaps its
 * content between the sign-in screen and the main shell. The services come from
 * {@code Main}, which is the only place that builds them, so this class never
 * creates a repository (Dependency Inversion).
 */
public final class FxApp extends Application {

    private static AuthService auth;
    private static Session session;
    private static DashboardService dashboard;

    private final FxTheme theme = new FxTheme();
    private Scene scene;

    /**
     * Starts the JavaFX interface and blocks until the window closes.
     *
     * @param authService      the sign-in rules
     * @param activeSession    where the signed-in user is stored
     * @param dashboardService the source of the home screen numbers
     */
    public static void run(AuthService authService, Session activeSession, DashboardService dashboardService) {
        auth = authService;
        session = activeSession;
        dashboard = dashboardService;
        launch(FxApp.class);
    }

    @Override
    public void start(Stage stage) {
        scene = new Scene(new Region(), 1360, 840);
        theme.attach(scene);
        stage.setScene(scene);
        stage.setTitle("NextFit");
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        Image icon = FxImages.load("/images/logo/nextfit-logo-256.png");
        if (icon != null) {
            stage.getIcons().add(icon);
        }
        showLogin();
        stage.show();
    }

    private void showLogin() {
        LoginView view = new LoginView(auth, session, this::showMain);
        setRoot(view);
        Platform.runLater(view::focusUsername);
    }

    private void showMain() {
        setRoot(new ShellView(session, dashboard, theme, this::logout));
    }

    private void logout() {
        session.end();
        showLogin();
    }

    private void setRoot(Parent root) {
        theme.mark(root);
        scene.setRoot(root);
        FadeTransition fade = new FadeTransition(Duration.millis(250), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}
