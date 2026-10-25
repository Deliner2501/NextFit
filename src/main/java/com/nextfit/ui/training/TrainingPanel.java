package com.nextfit.ui.training;

import com.nextfit.TrainingModule;
import com.nextfit.model.Role;
import com.nextfit.ui.ModulePanel;
import com.nextfit.ui.Theme;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import java.nio.file.Path;

/**
 * Main screen of the training module. It groups the screens of the module in tabs;
 * the other tabs (routines, assignments, sessions) are added here as they are built.
 */
public class TrainingPanel implements ModulePanel {

    private final JTabbedPane tabs = new JTabbedPane();
    private final ExerciseCatalogView catalog;
    private final TrainerListView trainers;
    private final RoutineListView routines;
    private final AssignmentView assignmentView;
    private final SessionView sessionView;

    /**
     * @param training the module with its services
     * @param dataDir  data folder; pictures go to its "images/exercises" subfolder
     */
    public TrainingPanel(TrainingModule training, Path dataDir) {
        this.catalog = new ExerciseCatalogView(training.exercises(), dataDir.resolve("images").resolve("exercises"));
        this.trainers = new TrainerListView(training.trainers());
        tabs.setBackground(Theme.BACKGROUND);
        tabs.addTab("Ejercicios", catalog);
        this.routines = new RoutineListView(training.routines(), training.trainers(), training.exercises());
        tabs.addTab("Rutinas", routines);
        this.assignmentView = new AssignmentView(training.assignments(), training.routines(), training.members());
        tabs.addTab("Asignación", assignmentView);
        this.sessionView = new SessionView(training.sessions(), training.assignments(),
                training.routines(), training.members());
        tabs.addTab("Sesiones", sessionView);
        tabs.addTab("Entrenadores", trainers);
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
        return "training";
    }

    @Override
    public JComponent view() {
        return tabs;
    }

    // Cada vez que se abre el módulo se recargan las pestañas.
    @Override
    public void onShow() {
        catalog.refresh();
        routines.refresh();
        assignmentView.refresh();
        sessionView.refresh();
        trainers.refresh();
    }

    // Solo el administrador y el entrenador pueden entrar a este módulo.
    @Override
    public boolean allowedFor(Role role) {
        return role == Role.ADMIN || role == Role.TRAINER;
    }
}