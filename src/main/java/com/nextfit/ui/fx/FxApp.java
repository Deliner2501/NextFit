package com.nextfit.ui.fx;

import com.nextfit.service.AuthService;
import com.nextfit.service.Session;
import com.nextfit.service.security.InactivityMonitor;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Entry point of the JavaFX interface. It owns the only window and swaps its
 * content between the sign-in screen and the main shell. The services come from
 * {@code Main}, which is the only place that builds them, so this class never
 * creates a repository (Dependency Inversion). It also closes the session when the
 * user has been inactive for too long.
 */
public final class FxApp extends Application {

    private static AuthService auth;
    private static Session session;
    private static Supplier<List<FxModule>> moduleFactory;
    private static List<FxModule> modules;

    private final FxTheme theme = new FxTheme();
    private final InactivityMonitor inactivity = new InactivityMonitor();
    private Scene scene;
    private IdleWatcher idle;

    /**
     * Starts the JavaFX interface and blocks until the window closes.
     *
     * @param authService      the sign-in rules
     * @param activeSession    where the signed-in user is stored
     * @param factory          builds every module, in sidebar order; it is called after JavaFX has started,
     *                         because controls cannot be created before that
     */
    public static void run(AuthService authService, Session activeSession, Supplier<List<FxModule>> factory) {
        auth = authService;
        session = activeSession;
        moduleFactory = factory;
        launch(FxApp.class);
    }

    @Override
    public void start(Stage stage) {
        modules = moduleFactory.get();
        scene = new Scene(new Region(), 1360, 840);
        theme.attach(scene);
        FxDialogs.useTheme(theme);
        idle = new IdleWatcher(scene, inactivity, this::logoutForInactivity);
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
        setRoot(new ShellView(session, modules, theme, this::logout));
        idle.start();
    }

    private void logout() {
        idle.stop();
        session.end();
        showLogin();
    }

    // Cierra la sesión porque el usuario dejó el computador solo; avisa en el login.
    private void logoutForInactivity() {
        closeDialogs();
        session.end();
        showLogin();
        Platform.runLater(() -> FxDialogs.message(scene.getRoot(), "Sesión cerrada",
                "Cerramos tu sesión por inactividad. Vuelve a entrar para continuar."));
    }

    // Si había un formulario abierto, se cierra para que no quede encima del login.
    private static void closeDialogs() {
        for (Window window : new ArrayList<>(Window.getWindows())) {
            if (window instanceof Stage stage && stage.getOwner() != null && stage.isShowing()) {
                stage.hide();
            }
        }
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