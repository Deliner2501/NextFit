package com.nextfit.ui.fx;

import com.nextfit.model.Trainer;
import com.nextfit.service.TrainerService;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Form to register a new trainer or edit an existing one. It never validates by itself:
 * it builds a {@link Trainer} and hands it to {@link TrainerService}, which applies the
 * rules; an error is shown inside the form by {@link FxDialogs#form}.
 */
final class TrainerForm {

    private final TrainerService service;
    private final Trainer existing;     // null cuando se registra uno nuevo
    private final Runnable onSaved;

    private final TextField nameField = new TextField();
    private final TextField documentField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final DatePicker birthPicker = new DatePicker();
    private final TextField specialtyField = new TextField();

    private TrainerForm(TrainerService service, Trainer existing, Runnable onSaved) {
        this.service = service;
        this.existing = existing;
        this.onSaved = onSaved;
    }

    /**
     * Opens the form.
     *
     * @param owner    a node of the window the form belongs to
     * @param service  the service that registers and updates trainers
     * @param existing the trainer to edit, or null to register a new one
     * @param onSaved  action to run after saving, to refresh the screen
     */
    static void open(Node owner, TrainerService service, Trainer existing, Runnable onSaved) {
        new TrainerForm(service, existing, onSaved).show(owner);
    }

    private void show(Node owner) {
        nameField.setPromptText("Nombre y apellido");
        documentField.setPromptText("Solo números");
        phoneField.setPromptText("Solo números");
        emailField.setPromptText("Opcional");
        specialtyField.setPromptText("Ej. Hipertrofia");
        for (TextField field : new TextField[] {nameField, documentField, phoneField, emailField, specialtyField}) {
            field.getStyleClass().add("nf-input");
            field.setPrefHeight(44);
        }
        birthPicker.setPrefHeight(44);
        birthPicker.setMaxWidth(Double.MAX_VALUE);
        birthPicker.setPromptText("AAAA-MM-DD");

        // EXPLICACIÓN: si se edita, el formulario arranca con los datos del entrenador.
        if (existing != null) {
            nameField.setText(existing.getFullName());
            documentField.setText(existing.getDocumentNumber());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            birthPicker.setValue(existing.getBirthDate());
            specialtyField.setText(existing.getSpecialty());
        }

        VBox documentField2 = FxDialogs.labeled("Documento", documentField);
        VBox phoneField2 = FxDialogs.labeled("Teléfono", phoneField);
        HBox.setHgrow(documentField2, Priority.ALWAYS);
        HBox.setHgrow(phoneField2, Priority.ALWAYS);
        VBox birthField = FxDialogs.labeled("Fecha de nacimiento", birthPicker);
        VBox specialtyBox = FxDialogs.labeled("Especialidad", specialtyField);
        HBox.setHgrow(birthField, Priority.ALWAYS);
        HBox.setHgrow(specialtyBox, Priority.ALWAYS);

        VBox fields = new VBox(12,
                FxDialogs.labeled("Nombre completo", nameField),
                new HBox(12, documentField2, phoneField2),
                FxDialogs.labeled("Correo", emailField),
                new HBox(12, birthField, specialtyBox));
        fields.setPrefWidth(460);

        FxDialogs.form(owner, existing == null ? "Nuevo entrenador" : "Editar entrenador",
                "Guardar", fields, this::save);
    }

    // Lo llama el botón Guardar; un error de reglas lo muestra FxDialogs dentro del formulario.
    private void save() {
        LocalDate birthDate = birthPicker.getValue();   // vacía: el validador responde "es obligatoria"
        Trainer trainer = new Trainer(
                existing == null ? 0 : existing.getId(),
                nameField.getText().trim(),
                documentField.getText().trim(),
                phoneField.getText().trim(),
                emailField.getText().trim(),
                birthDate,
                specialtyField.getText().trim(),
                existing == null ? "" : existing.getPhotoPath(),
                existing == null || existing.isActive());
        if (existing == null) {
            service.register(trainer);
        } else {
            service.update(trainer);
        }
        onSaved.run();
    }
}