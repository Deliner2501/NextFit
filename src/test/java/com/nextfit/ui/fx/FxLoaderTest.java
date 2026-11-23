package com.nextfit.ui.fx;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("hasDisplay")
class FxLoaderTest {
    // GitHub's default Linux runner has no graphical display for the JavaFX toolkit.
    static boolean hasDisplay() {
        return !System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux")
                || System.getenv("DISPLAY") != null;
    }

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @Test
    void slowReadsLeaveTheInterfaceFreeAndOnlyTheLatestFilterIsDisplayed() throws Exception {
        FxLoader loader = new FxLoader();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        List<Integer> displayed = new ArrayList<>();
        CompletableFuture<Integer> completed = new CompletableFuture<>();
        try {
            onFx(() -> {
                loader.load(() -> {
                    assertFalse(Platform.isFxApplicationThread());
                    reads.incrementAndGet();
                    started.countDown();
                    try {
                        assertTrue(release.await(10, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return 1;
                }, displayed::add, completed::completeExceptionally);
                return null;
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            // This callback must run while the database query is still blocked.
            onFx(() -> {
                loader.load(() -> { reads.incrementAndGet(); return 2; }, displayed::add,
                        completed::completeExceptionally);
                loader.load(() -> { reads.incrementAndGet(); return 3; }, value -> {
                    assertTrue(Platform.isFxApplicationThread());
                    displayed.add(value);
                    completed.complete(value);
                }, completed::completeExceptionally);
                return null;
            });
            release.countDown();
            assertEquals(3, completed.get(5, TimeUnit.SECONDS));
            assertEquals(List.of(3), displayed);
            assertEquals(2, reads.get());
        } finally {
            release.countDown();
        }
    }

    @Test
    void aFailedReadCanBeRetried() throws Exception {
        FxLoader loader = new FxLoader();
        CompletableFuture<Throwable> failure = new CompletableFuture<>();
        onFx(() -> {
            loader.load(() -> { throw new IllegalStateException("Oracle unavailable"); },
                    ignored -> fail("A failed query must not display data"), failure::complete);
            return null;
        });
        assertEquals("Oracle unavailable", failure.get(5, TimeUnit.SECONDS).getMessage());
        CompletableFuture<Integer> result = new CompletableFuture<>();
        onFx(() -> {
            loader.load(() -> 42, result::complete, result::completeExceptionally);
            return null;
        });
        assertEquals(42, result.get(5, TimeUnit.SECONDS));
    }

    private static <T> T onFx(Callable<T> work) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.call());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.get(3, TimeUnit.SECONDS);
    }
}
