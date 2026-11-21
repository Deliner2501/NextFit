package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExerciseValidatorTest {

    private final ExerciseValidator validator = new ExerciseValidator();

    private String badField(Exercise exercise) {
        return assertThrows(InvalidDataException.class, () -> validator.validate(exercise)).getFieldName();
    }

    @Test
    void aCompleteExerciseIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(
                new Exercise(0, "Remo con mancuerna", "Espalda", MuscleGroup.BACK, Difficulty.values()[0], "")));
    }

    @Test
    void theNameIsRequiredAndBetweenThreeAndSixtyCharacters() {
        assertEquals("nombre del ejercicio",
                badField(new Exercise(0, " ", "", MuscleGroup.BACK, Difficulty.values()[0], "")));
        assertThrows(InvalidDataException.class, () -> validator.validateName("ab"));
        assertDoesNotThrow(() -> validator.validateName("abc"));
        assertDoesNotThrow(() -> validator.validateName("a".repeat(60)));
        assertThrows(InvalidDataException.class, () -> validator.validateName("a".repeat(61)));
    }

    @Test
    void theDescriptionIsOptionalAndLimited() {
        assertDoesNotThrow(() -> validator.validateDescription(null));
        assertDoesNotThrow(() -> validator.validateDescription("x".repeat(300)));
        assertThrows(InvalidDataException.class, () -> validator.validateDescription("x".repeat(301)));
    }

    @Test
    void theMuscleGroupAndTheDifficultyAreRequired() {
        assertEquals("grupo muscular",
                badField(new Exercise(0, "Remo", "", null, Difficulty.values()[0], "")));
        assertEquals("dificultad",
                badField(new Exercise(0, "Remo", "", MuscleGroup.BACK, null, "")));
    }
}
