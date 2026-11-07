package com.nextfit.ui.fx;

import com.nextfit.TrainingModule;
import com.nextfit.model.Role;
import com.nextfit.service.suggest.SuggestionService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;

import java.nio.file.Path;

/**
 * Training module in JavaFX. It groups its screens in tabs: exercises, routines,
 * assignments, sessions and trainers. It is for the administrator and the trainer.
 * The rules live in the training services; this class only hosts the screens.
 */
public final class TrainingFxModule implements FxModule {

    private final TabPane tabs = new TabPane();
    private final ExercisesPane exercises;
    private final RoutinesPane routines;
    private final AssignmentPane assignments;
    private final SessionsPane sessions;

    /**
     * @param training     the object that builds and holds the training services
     * @param suggestions  the service that builds suggested routines
     * @param imagesFolder folder where the exercise pictures are stored
     */
    public TrainingFxModule(TrainingModule training, SuggestionService suggestions, Path imagesFolder) {
        this.exercises = new ExercisesPane(training.exercises(), imagesFolder);
        this.routines = new RoutinesPane(training.routines(), training.trainers(), training.exercises(),
                training.members(), suggestions);
        this.assignments = new AssignmentPane(training.assignments(), training.routines(), training.members());
        this.sessions = new SessionsPane(training.sessions(), training.assignments(), training.routines(),
                training.members());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("nf-tabs");
        tabs.getTabs().addAll(tab("Ejercicios", exercises), tab("Rutinas", routines),
                tab("Asignación", assignments), tab("Sesiones", sessions));
        // Lo creado o editado en una pestaña debe verse en las otras.
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> refreshTabs());
    }

    @Override
    public String id() {
        return "training";
    }

    @Override
    public String title() {
        return "Entrenamiento";
    }

    @Override
    public String icon() {
        return FxIcons.TRAINING;
    }

    @Override
    public Node view() {
        return tabs;
    }

    @Override
    public void onShow() {
        refreshTabs();
    }

    /** Only the administrator and the trainer can open this module. */
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.TRAINER;
    }

    private void refreshTabs() {
        exercises.refresh();
        routines.refresh();
        assignments.refresh();
        sessions.refresh();
    }

    private static Tab tab(String title, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPadding(new Insets(18, 0, 0, 0));
        Tab tab = new Tab(title, holder);
        tab.setClosable(false);
        return tab;
    }
}