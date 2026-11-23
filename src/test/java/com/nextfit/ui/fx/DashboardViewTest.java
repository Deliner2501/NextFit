package com.nextfit.ui.fx;

import com.nextfit.service.DashboardService;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIf("hasDisplay")
class DashboardViewTest {
    // GitHub's default Linux runner has no graphical display for the JavaFX toolkit.
    static boolean hasDisplay() {
        return !System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux")
                || System.getenv("DISPLAY") != null;
    }


    @Test
    void aSlowDatabaseDoesNotBlockTheInterfaceThread() throws Exception {
        CountDownLatch toolkitReady = new CountDownLatch(1);
        try {
            Platform.startup(toolkitReady::countDown);
        } catch (IllegalStateException alreadyStarted) {
            toolkitReady.countDown();
        }
        assertTrue(toolkitReady.await(10, TimeUnit.SECONDS));

        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);
        CountDownLatch refreshReturned = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        DashboardService slowDatabase = new DashboardService(null, null, null, null) {
            @Override
            public Summary summary() {
                queryStarted.countDown();
                try {
                    releaseQuery.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new Summary(LocalDate.now(), 0, 0, 0, 0, 0, 0, 0,
                        Map.of(), Map.of(), 0, 0);
            }
        };
        try {
            Platform.runLater(() -> {
                try {
                    new DashboardView(slowDatabase).refresh();
                } catch (Throwable e) {
                    failure.set(e);
                } finally {
                    refreshReturned.countDown();
                }
            });
            assertTrue(queryStarted.await(5, TimeUnit.SECONDS));
            assertTrue(refreshReturned.await(2, TimeUnit.SECONDS),
                    "The interface must remain available while Oracle is responding");
            assertNull(failure.get());
        } finally {
            releaseQuery.countDown();
        }
    }
}
