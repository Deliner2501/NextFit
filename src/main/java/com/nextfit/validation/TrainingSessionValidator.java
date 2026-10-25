package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.TrainingSession;

import java.time.LocalDate;

/** Checks that the data of a {@link TrainingSession} is valid before it is stored. */
public class TrainingSessionValidator {

    private static final int DAY_NAME_MAX = 60;
    private static final int DURATION_MIN = 5;
    private static final int DURATION_MAX = 300;
    private static final int NOTES_MAX = 200;

    /** Validates every field; throws InvalidDataException at the first invalid one. */
    public void validate(TrainingSession session) {
        validateIds(session.getMemberId(), session.getAssignmentId());
        validateDayName(session.getDayName());
        validateDate(session.getDate());
        validateDuration(session.getDurationMinutes());
        validateNotes(session.getNotes());
    }

    public void validateIds(long memberId, long assignmentId) {
        if (memberId <= 0) {
            throw new InvalidDataException("socio", "es obligatorio");
        }
        if (assignmentId <= 0) {
            throw new InvalidDataException("asignación", "es obligatoria");
        }
    }

    public void validateDayName(String dayName) {
        if (dayName == null || dayName.isBlank()) {
            throw new InvalidDataException("día de la rutina", "no puede estar vacío");
        }
        if (dayName.length() > DAY_NAME_MAX) {
            throw new InvalidDataException("día de la rutina",
                    "no puede superar los " + DAY_NAME_MAX + " caracteres");
        }
    }

    public void validateDate(LocalDate date) {
        if (date == null) {
            throw new InvalidDataException("fecha", "es obligatoria");
        }
        // No se puede registrar un entrenamiento que aún no ocurre.
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidDataException("fecha", "no puede ser futura");
        }
    }

    public void validateDuration(int minutes) {
        if (minutes < DURATION_MIN || minutes > DURATION_MAX) {
            throw new InvalidDataException("duración",
                    "debe estar entre " + DURATION_MIN + " y " + DURATION_MAX + " minutos");
        }
    }

    public void validateNotes(String notes) {
        // La nota es opcional: solo se revisa el largo si viene escrita.
        if (notes != null && notes.length() > NOTES_MAX) {
            throw new InvalidDataException("notas",
                    "no pueden superar los " + NOTES_MAX + " caracteres");
        }
    }
}