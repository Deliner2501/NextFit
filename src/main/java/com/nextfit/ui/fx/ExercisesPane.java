package com.nextfit.ui.fx;

import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.service.ExerciseService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Gallery of the exercise catalog: a search box, two filters and one card with a photo
 * for each exercise. A click on a card edits that exercise and the button creates a new
 * one. The rules live in {@link ExerciseService}; this class only collects the search
 * text, the filters and the clicks, and shows the result.
 */
final class ExercisesPane extends VBox {

    private static final double CARD_WIDTH = 220;
    private static final double PICTURE_HEIGHT = 140;
    private static final double CARD_HEIGHT = 220;
    private static final double CARD_ARC = 32;

    private final ExerciseService service;
    private final Path imagesFolder;
    private final Label countLabel = new Label();
    private final TextField searchField = new TextField();
    private final ComboBox<String> groupFilter = new ComboBox<>();
    private final ComboBox<String> difficultyFilter = new ComboBox<>();
    private final FlowPane grid = new FlowPane(16, 16);
    private final ScrollPane scroll = new ScrollPane(grid);

    /**
     * @param service      the service that manages the exercise catalog
     * @param imagesFolder folder where the pictures chosen in the form are stored
     */
    ExercisesPane(ExerciseService service, Path imagesFolder) {
        super(16);
        this.service = service;
        this.imagesFolder = imagesFolder;
        grid.setPadding(new Insets(2));
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("nf-scroll");
        getChildren().addAll(buildHeader(), scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        refresh();
    }

    private Node buildHeader() {
        Label title = new Label("Catálogo de ejercicios");
        title.getStyleClass().add("nf-section-title");
        countLabel.getStyleClass().add("nf-muted");
        VBox titles = new VBox(2, title, countLabel);
        HBox.setHgrow(titles, Priority.ALWAYS);

        searchField.setPromptText("Buscar por nombre");
        searchField.getStyleClass().add("nf-input");
        searchField.setPrefSize(240, 44);

        // EXPLICACIÓN: la posición 0 de cada lista es "Todos" y significa "sin filtro";
        // las demás posiciones siguen el orden de los valores del enum.
        groupFilter.getItems().add("Todos los grupos");
        for (MuscleGroup group : MuscleGroup.values()) {
            groupFilter.getItems().add(group.getLabel());
        }
        groupFilter.getSelectionModel().select(0);
        groupFilter.setPrefHeight(44);

        difficultyFilter.getItems().add("Todas las dificultades");
        for (Difficulty difficulty : Difficulty.values()) {
            difficultyFilter.getItems().add(difficulty.getLabel());
        }
        difficultyFilter.getSelectionModel().select(0);
        difficultyFilter.setPrefHeight(44);

        searchField.textProperty().addListener((obs, old, now) -> refresh());
        groupFilter.getSelectionModel().selectedIndexProperty().addListener((obs, old, now) -> refresh());
        difficultyFilter.getSelectionModel().selectedIndexProperty().addListener((obs, old, now) -> refresh());

        Button create = new Button("+ Nuevo ejercicio");
        create.getStyleClass().add("nf-primary-small");
        create.setOnAction(e -> openForm(null));

        HBox header = new HBox(10, titles, searchField, groupFilter, difficultyFilter, create);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /** Reloads the cards from the service, applying the search text and the filters. */
    void refresh() {
        int groupIndex = groupFilter.getSelectionModel().getSelectedIndex();
        int difficultyIndex = difficultyFilter.getSelectionModel().getSelectedIndex();
        MuscleGroup group = groupIndex <= 0 ? null : MuscleGroup.values()[groupIndex - 1];
        Difficulty difficulty = difficultyIndex <= 0 ? null : Difficulty.values()[difficultyIndex - 1];

        List<Exercise> shown = service.search(searchField.getText(), group, difficulty);
        countLabel.setText(shown.size() == 1 ? "1 ejercicio" : shown.size() + " ejercicios");

        grid.getChildren().clear();
        if (shown.isEmpty()) {
            Label empty = new Label("No hay ejercicios con esos filtros. Crea uno con «+ Nuevo ejercicio».");
            empty.getStyleClass().add("nf-muted");
            grid.getChildren().add(empty);
            return;
        }
        for (Exercise exercise : shown) {
            grid.getChildren().add(card(exercise));
        }
    }

    // Abre el formulario para crear (exercise null) o editar un ejercicio.
    private void openForm(Exercise exercise) {
        ExerciseForm.open(this, service, imagesFolder, exercise, this::refresh);
    }

    private Node card(Exercise exercise) {
        Label name = new Label(exercise.getName());
        name.getStyleClass().add("nf-card-title");
        name.setWrapText(true);
        name.setMaxHeight(44);
        Label detail = new Label(exercise.getMuscleGroup().getLabel() + " · "
                + exercise.getDifficulty().getLabel());
        detail.getStyleClass().add("nf-muted");
        VBox text = new VBox(4, name, detail);
        text.setPadding(new Insets(12));

        VBox card = new VBox(picture(exercise), text);
        card.getStyleClass().add("nf-exercise-card");
        card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        card.setOnMouseClicked(e -> openForm(exercise));
        // EXPLICACIÓN: el recorte redondea las esquinas también en la foto.
        Rectangle clip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        clip.setArcWidth(CARD_ARC);
        clip.setArcHeight(CARD_ARC);
        card.setClip(clip);
        return card;
    }

    // La foto se recorta para llenar todo el espacio (como "cover" en CSS).
    private Node picture(Exercise exercise) {
        Image image = pictureOf(exercise);
        if (image == null) {
            Region empty = new Region();
            empty.setMinHeight(PICTURE_HEIGHT);
            empty.getStyleClass().add("nf-exercise-empty");
            return empty;
        }
        double scale = Math.max(CARD_WIDTH / image.getWidth(), PICTURE_HEIGHT / image.getHeight());
        double viewWidth = CARD_WIDTH / scale;
        double viewHeight = PICTURE_HEIGHT / scale;
        ImageView view = new ImageView(image);
        view.setViewport(new Rectangle2D((image.getWidth() - viewWidth) / 2,
                (image.getHeight() - viewHeight) / 2, viewWidth, viewHeight));
        view.setFitWidth(CARD_WIDTH);
        view.setFitHeight(PICTURE_HEIGHT);
        view.setSmooth(true);
        return view;
    }

    // Foto propia del ejercicio; si no tiene o no se puede leer, la foto de su grupo muscular.
    static Image pictureOf(Exercise exercise) {
        String path = exercise.getImagePath();
        Image image = null;
        if (path != null && !path.isBlank()) {
            image = path.startsWith("/") ? FxImages.load(path) : fromFile(path);
        }
        if (image != null) {
            return image;
        }
        return FxImages.load("/images/exercises/" + exercise.getMuscleGroup().name().toLowerCase() + ".jpg");
    }

    private static Image fromFile(String path) {
        try {
            Path file = Path.of(path);
            if (Files.isRegularFile(file)) {
                return new Image(file.toUri().toString(), false);
            }
        } catch (RuntimeException e) {
            // EXPLICACIÓN: una ruta como "art:CHEST" no es un archivo válido en Windows;
            // en ese caso se usa la foto del grupo muscular.
        }
        return null;
    }
}