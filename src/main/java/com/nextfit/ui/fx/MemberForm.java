package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import com.nextfit.service.MemberService;
import com.nextfit.util.PhotoStorage;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Form to register a new member or edit an existing one. It never validates by itself:
 * it builds a {@link Member} and hands it to {@link MemberService}, which applies the
 * rules; an error is shown inside the form by {@link FxDialogs#form}. The photo is copied
 * by {@link PhotoStorage}.
 */
final class MemberForm {

    private final MemberService service;
    private final PhotoStorage photoStorage;
    private final Member existing;      // null when a new member is registered
    private final Runnable onSaved;

    private final TextField nameField = new TextField();
    private final TextField documentField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final DatePicker birthPicker = new DatePicker();
    private final TextField emergencyField = new TextField();
    private final Label photoLabel = new Label();
    private Path chosenPhoto;

    private MemberForm(MemberService service, PhotoStorage photoStorage, Member existing, Runnable onSaved) {
        this.service = service;
        this.photoStorage = photoStorage;
        this.existing = existing;
        this.onSaved = onSaved;
    }

    /**
     * Opens the form.
     *
     * @param owner        a node of the window the form belongs to
     * @param service      the service that registers and updates members
     * @param photoStorage where the chosen photo is copied
     * @param existing     the member to edit, or null to register a new one
     * @param onSaved      action to run after saving, to refresh the screen
     */
    static void open(Node owner, MemberService service, PhotoStorage photoStorage, Member existing,
                     Runnable onSaved) {
        new MemberForm(service, photoStorage, existing, onSaved).show(owner);
    }

    private void show(Node owner) {
        nameField.setPromptText("Nombre y apellido");
        documentField.setPromptText("Solo números");
        phoneField.setPromptText("Solo números");
        emailField.setPromptText("correo@ejemplo.com");
        emergencyField.setPromptText("Nombre y teléfono");
        for (TextField field : new TextField[] {nameField, documentField, phoneField, emailField, emergencyField}) {
            field.getStyleClass().add("nf-input");
            field.setPrefHeight(44);
        }
        birthPicker.setPrefHeight(44);
        birthPicker.setMaxWidth(Double.MAX_VALUE);
        birthPicker.setPromptText("AAAA-MM-DD");

        // When editing, the form starts with the data of the member.
        if (existing != null) {
            nameField.setText(existing.getFullName());
            documentField.setText(existing.getDocumentNumber());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            birthPicker.setValue(existing.getBirthDate());
            emergencyField.setText(existing.getEmergencyContact());
        }
        photoLabel.getStyleClass().add("nf-muted");
        photoLabel.setText(existing != null && existing.getPhotoPath() != null && !existing.getPhotoPath().isBlank()
                ? "Foto actual" : "Sin foto");
        Button choosePhoto = new Button("Elegir foto");
        choosePhoto.getStyleClass().add("nf-secondary");
        choosePhoto.setOnAction(e -> choosePhoto(choosePhoto));

        VBox documentBox = FxDialogs.labeled("Documento", documentField);
        VBox phoneBox = FxDialogs.labeled("Teléfono", phoneField);
        HBox.setHgrow(documentBox, Priority.ALWAYS);
        HBox.setHgrow(phoneBox, Priority.ALWAYS);
        VBox birthBox = FxDialogs.labeled("Fecha de nacimiento", birthPicker);
        VBox emergencyBox = FxDialogs.labeled("Contacto de emergencia", emergencyField);
        HBox.setHgrow(birthBox, Priority.ALWAYS);
        HBox.setHgrow(emergencyBox, Priority.ALWAYS);
        HBox photoRow = new HBox(12, choosePhoto, photoLabel);
        photoRow.setAlignment(Pos.CENTER_LEFT);

        VBox fields = new VBox(12,
                FxDialogs.labeled("Nombre completo", nameField),
                new HBox(12, documentBox, phoneBox),
                FxDialogs.labeled("Correo", emailField),
                new HBox(12, birthBox, emergencyBox),
                FxDialogs.labeled("Foto (JPG o PNG, opcional)", photoRow));
        fields.setPrefWidth(500);

        FxDialogs.form(owner, existing == null ? "Nuevo socio" : "Editar socio", "Guardar", fields, this::save);
    }

    // Lets the user pick an image; it is copied only when the member is saved.
    private void choosePhoto(Button source) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir foto del socio");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes JPG o PNG", "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(source.getScene().getWindow());
        if (file != null) {
            chosenPhoto = file.toPath();
            photoLabel.setText(file.getName());
        }
    }

    // Called by the Save button; a rule error is shown by FxDialogs inside the form.
    private void save() {
        String document = documentField.getText().trim();
        String photoPath = existing == null ? "" : existing.getPhotoPath();
        if (chosenPhoto != null && !document.isEmpty()) {
            photoPath = photoStorage.copy(chosenPhoto, document);
        }
        Member member = new Member(
                existing == null ? 0 : existing.getId(),
                nameField.getText().trim(),
                document,
                phoneField.getText().trim(),
                emailField.getText().trim(),
                birthPicker.getValue(),     // empty: the validator answers "is required"
                existing == null ? LocalDate.now() : existing.getJoinDate(),
                photoPath,
                emergencyField.getText().trim(),
                existing == null || existing.isActive());
        if (existing == null) {
            service.register(member);
        } else {
            service.update(member);
        }
        onSaved.run();
    }
}
