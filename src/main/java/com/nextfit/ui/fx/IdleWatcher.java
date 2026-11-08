package com.nextfit.ui.fx;

import com.nextfit.service.security.InactivityMonitor;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

/**
 * Watches the window for activity and runs an action when the user has been inactive for too
 * long. It only connects the events of the window with an {@link InactivityMonitor}, which
 * decides when the time is over, and checks it every few seconds.
 */
final class IdleWatcher {

    private static final int CHECK_SECONDS = 5;

    private final InactivityMonitor monitor;
    private final Runnable onTimeout;
    private final Timeline timeline;

    /**
     * @param scene     the window to watch
     * @param monitor   the object that tells when the inactivity is over
     * @param onTimeout the action to run when the user has been inactive for too long
     */
    IdleWatcher(Scene scene, InactivityMonitor monitor, Runnable onTimeout) {
        this.monitor = monitor;
        this.onTimeout = onTimeout;
        // EXPLICACIÓN: se usa un filtro de eventos para ver la actividad aunque otro control la consuma.
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> monitor.touch());
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> monitor.touch());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> monitor.touch());
        scene.addEventFilter(ScrollEvent.SCROLL, e -> monitor.touch());
        timeline = new Timeline(new KeyFrame(Duration.seconds(CHECK_SECONDS), e -> check()));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    /** Starts watching, counting from now. */
    void start() {
        monitor.touch();
        timeline.play();
    }

    /** Stops watching, for example when nobody is signed in. */
    void stop() {
        timeline.stop();
    }

    private void check() {
        if (monitor.isExpired()) {
            timeline.stop();
            onTimeout.run();
        }
    }
}