package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("hasDisplay")
class MembersPaneTest {
    // GitHub's default Linux runner has no graphical display for the JavaFX toolkit.
    static boolean hasDisplay() {
        return !System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux")
                || System.getenv("DISPLAY") != null;
    }

    @Test
    void loadingDoesNotBlockNavigationAndSearchingDoesNotQueryAgain() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch returned = new CountDownLatch(1);
        CountDownLatch displayed = new CountDownLatch(1);
        AtomicInteger queries = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        MemberService service = new MemberService(null, null) {
            @Override
            public List<Member> findAll() {
                queries.incrementAndGet();
                started.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return List.of();
            }
        };
        try {
            Platform.runLater(() -> {
                try {
                    MembersPane pane = new MembersPane(service, null, () -> true, member -> {});
                    pane.refresh(); // A second onShow while loading must not duplicate the query.
                    TableView<?> table = (TableView<?>) pane.getChildren().get(1);
                    // The loading label changes only after the worker result reaches the UI thread.
                    javafx.scene.control.Label label = (javafx.scene.control.Label)
                            ((javafx.scene.layout.VBox) ((javafx.scene.layout.HBox)
                                    pane.getChildren().get(0)).getChildren().get(0)).getChildren().get(0);
                    label.textProperty().addListener((obs, old, value) -> {
                        if (value.startsWith("0 socios")) {
                            try {
                                TextField search = (TextField) ((javafx.scene.layout.HBox)
                                        pane.getChildren().get(0)).getChildren().stream()
                                        .filter(node -> node instanceof TextField).findFirst().orElseThrow();
                                search.setText("Valentina");
                            } catch (Throwable e) {
                                failure.set(e);
                            } finally {
                                displayed.countDown();
                            }
                        }
                    });
                } catch (Throwable e) {
                    failure.set(e);
                } finally {
                    returned.countDown();
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(returned.await(2, TimeUnit.SECONDS));
            assertNull(failure.get());
            release.countDown();
            assertTrue(displayed.await(5, TimeUnit.SECONDS));
            assertNull(failure.get());
            assertEquals(1, queries.get());
        } finally {
            release.countDown();
        }
    }
}
