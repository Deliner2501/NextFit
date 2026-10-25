package com.nextfit.model;

import java.time.LocalDate;

/**
 * A gym trainer. Extends {@link Person}, so it inherits the personal data
 * (name, document, phone, email, birth date) and adds what is specific to a trainer.
 * Immutable: to change a value, create a new copy with a {@code withX} method.
 */
public class Trainer extends Person {

    private final String specialty;
    private final String photoPath;
    private final boolean active;

    /**
     * Creates a trainer.
     *
     * @param id       unique id (0 means "not stored yet")
     * @param specialty main area of the trainer (for example "Hipertrofia")
     * @param photoPath path of the photo image, may be empty
     * @param active   whether the trainer currently works at the gym
     */
    public Trainer(long id, String fullName, String documentNumber, String phone, String email,
                   LocalDate birthDate, String specialty, String photoPath, boolean active) {
        // Los datos personales los guarda Person; aquí solo guardamos lo propio del entrenador.
        super(id, fullName, documentNumber, phone, email, birthDate);
        this.specialty = specialty;
        this.photoPath = photoPath;
        this.active = active;
    }

    @Override
    public String getPersonType() {
        return "Entrenador";
    }

    /** Returns a copy of this trainer with the given id (used by the repository on first save). */
    public Trainer withId(long newId) {
        return new Trainer(newId, getFullName(), getDocumentNumber(), getPhone(), getEmail(),
                getBirthDate(), specialty, photoPath, active);
    }

    /** Returns a copy of this trainer with the given active flag. */
    public Trainer withActive(boolean newActive) {
        return new Trainer(getId(), getFullName(), getDocumentNumber(), getPhone(), getEmail(),
                getBirthDate(), specialty, photoPath, newActive);
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public boolean isActive() {
        return active;
    }
}