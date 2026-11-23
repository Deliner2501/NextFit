package com.nextfit.ui.fx;

import javafx.concurrent.Task;
import javafx.scene.Node;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Loads data off the interface thread; only the latest requested result is displayed. */
final class FxLoader {
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(4, runnable -> {
        Thread worker = new Thread(runnable, "screen-data");
        worker.setDaemon(true);
        return worker;
    });

    private boolean running;
    private Runnable pending;

    <T> void load(Node owner, Supplier<T> query, Consumer<T> display) {
        load(query, display, error -> FxDialogs.message(owner, "No se pudieron cargar los datos",
                "Vuelve a entrar en esta sección para reintentar. "
                        + (error.getMessage() == null ? "" : error.getMessage())));
    }

    <T> void load(Supplier<T> query, Consumer<T> display, Consumer<Throwable> failure) {
        if (running) {
            pending = () -> load(query, display, failure);
            return;
        }
        running = true;
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return query.get();
            }
        };
        task.setOnSucceeded(event -> finish(() -> display.accept(task.getValue())));
        task.setOnFailed(event -> finish(() -> failure.accept(task.getException())));
        WORKERS.execute(task);
    }

    private void finish(Runnable result) {
        running = false;
        Runnable next = pending;
        pending = null;
        if (next == null) {
            result.run();
        } else {
            next.run();
        }
    }
}
