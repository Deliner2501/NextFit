package com.nextfit.ui.fx;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.service.ExerciseService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

/**
 * Form to create a new exercise or edit an existing one. It never validates by itself:
 * it builds an {@link Exercise} and hands it to {@link ExerciseService}, which applies
 * the rules; an error is shown inside the form by {@link FxDialogs#form}.
 */
final class ExerciseForm {

    private static final double PREVIEW_WIDTH = 200;
    private static final double PREVIEW_HEIGHT = 120;

    private final Node owner;
    private final ExerciseService service;
    private final Path imagesFolder;
    private final Exercise existing;      // null cuando se está creando uno nuevo
    private final Runnable onSaved;

    private final TextField nameField = new TextField();
    private final TextArea descriptionArea = new TextArea();
    private final ComboBox<MuscleGroup> groupBox = new ComboBox<>();
    private final ComboBox<Difficulty> difficultyBox = new ComboBox<>();
    private final ImageView preview = new ImageView();
    private String imagePath;
    private boolean newImageChosen;

    private ExerciseForm(Node owner, ExerciseService service, Path imagesFolder,
                         Exercise existing, Runnable onSaved) {
        this.owner = owner;
        this.service = service;
        this.imagesFolder = imagesFolder;
        this.existing = existing;
        this.onSaved = onSaved;
    }

    /**
     * Opens the form.
     *
     * @param owner        a node of the window the form belongs to
     * @param service      the service that creates and updates exercises
     * @param imagesFolder folder where the chosen pictures are copied
     * @param existing     the exercise to edit, or null to create a new one
     * @param onSaved      action to run after saving, to refresh the screen
     */
    static void open(Node owner, ExerciseService service, Path imagesFolder,
                     Exercise existing, Runnable onSaved) {
        new ExerciseForm(owner, service, imagesFolder, existing, onSaved).show();
    }

    private void show() {
        nameField.getStyleClass().add("nf-input");
        nameField.setPrefHeight(44);
        nameField.setPromptText("Ej. Press de banca");

        descriptionArea.getStyleClass().add("nf-textarea");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);
        descriptionArea.setPromptText("Cómo se hace y qué músculos trabaja (máximo 300 caracteres)");

        groupBox.getItems().addAll(MuscleGroup.values());
        groupBox.setConverter(converter(MuscleGroup::getLabel));
        groupBox.setMaxWidth(Double.MAX_VALUE);
        difficultyBox.getItems().addAll(Difficulty.values());
        difficultyBox.setConverter(converter(Difficulty::getLabel));
        difficultyBox.setMaxWidth(Double.MAX_VALUE);

        // EXPLICACIÓN: si se edita, el formulario arranca con los datos del ejercicio;
        // si se crea, arranca con el primer grupo y la dificultad más baja.
        groupBox.setValue(existing == null ? MuscleGroup.values()[0] : existing.getMuscleGroup());
        difficultyBox.setValue(existing == null ? Difficulty.BEGINNER : existing.getDifficulty());
        if (existing != null) {
            nameField.setText(existing.getName());
            descriptionArea.setText(existing.getDescription() == null ? "" : existing.getDescription());
        }
        imagePath = existing == null || existing.getImagePath() == null ? "" : existing.getImagePath();

        preview.setFitWidth(PREVIEW_WIDTH);
        preview.setFitHeight(PREVIEW_HEIGHT);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        Button choose = new Button("Elegir imagen…");
        choose.getStyleClass().add("nf-secondary");
        choose.setOnAction(e -> chooseImage());
        Label hint = new Label("Si no eliges una foto, se usa la de su grupo muscular.");
        hint.getStyleClass().add("nf-muted-small");
        hint.setWrapText(true);
        // La vista previa cambia si el usuario cambia de grupo y el ejercicio no tiene foto propia.
        groupBox.valueProperty().addListener((obs, old, now) -> showPreview());
        showPreview();

        VBox groupField = FxDialogs.labeled("Grupo muscular", groupBox);
        VBox difficultyField = FxDialogs.labeled("Dificultad", difficultyBox);
        HBox.setHgrow(groupField, Priority.ALWAYS);
        HBox.setHgrow(difficultyField, Priority.ALWAYS);

        VBox fields = new VBox(12,
                FxDialogs.labeled("Nombre", nameField),
                FxDialogs.labeled("Descripción", descriptionArea),
                new HBox(12, groupField, difficultyField),
                FxDialogs.labeled("Foto", new VBox(8, preview, choose, hint)));
        fields.setPrefWidth(440);

        FxDialogs.form(owner, existing == null ? "Nuevo ejercicio" : "Editar ejercicio",
                "Guardar", fields, this::save);
    }

    private void showPreview() {
        Exercise probe = new Exercise(0, "", "", groupBox.getValue(), difficultyBox.getValue(), imagePath);
        preview.setImage(ExercisesPane.pictureOf(probe));
    }

    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir foto del ejercicio");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes (png, jpg, jpeg)", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(owner.getScene().getWindow());
        if (file != null) {
            imagePath = file.getAbsolutePath();
            newImageChosen = true;
            showPreview();
        }
    }

    // Lo llama el botón Guardar; un error de reglas lo muestra FxDialogs dentro del formulario.
    private void save() {
        String finalImagePath = newImageChosen ? copyToFolder(imagePath) : imagePath;
        Exercise exercise = new Exercise(
                existing == null ? 0 : existing.getId(),
                nameField.getText().trim(),
                descriptionArea.getText().trim(),
                groupBox.getValue(),
                difficultyBox.getValue(),
                finalImagePath);
        if (existing == null) {
            service.create(exercise);
        } else {
            service.update(exercise);
        }
        onSaved.run();
    }

    // Copia la foto elegida a la carpeta de imágenes del programa y devuelve la nueva ruta.
    private String copyToFolder(String sourcePath) {
        try {
            Files.createDirectories(imagesFolder);
            String name = Path.of(sourcePath).getFileName().toString();
            String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".png";
            Path target = imagesFolder.resolve("exercise-" + System.currentTimeMillis() + extension);
            Files.copy(Path.of(sourcePath), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new PersistenceException("No se pudo copiar la imagen", e);
        }
    }

    // Muestra el texto de cada valor del enum (por ejemplo "Pecho" en vez de CHEST).
    private static <T> StringConverter<T> converter(Function<T, String> text) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : text.apply(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        };
    }
}