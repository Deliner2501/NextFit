package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.ResourceNotFoundException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.validation.ExerciseValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the exercise catalog rules, using a real CSV file in a temporary folder. */
class ExerciseServiceTest {

    private ExerciseService service;

    // @TempDir da una carpeta nueva y vacía en cada prueba: ninguna prueba afecta a otra.
    @BeforeEach
    void setUp(@TempDir Path folder) {
        service = new ExerciseService(
                new CsvExerciseRepository(folder.resolve("exercises.csv")), new ExerciseValidator());
    }

    // Crea un ejercicio de prueba con el nombre y grupo muscular indicados.
    private Exercise exercise(String name, MuscleGroup group) {
        return new Exercise(0, name, "Descripción", group, Difficulty.BEGINNER, "");
    }

    @Test
    void createAssignsAnId() {
        Exercise saved = service.create(exercise("Press de banca", MuscleGroup.CHEST));
        assertEquals(1, saved.getId());
    }

    @Test
    void createRejectsRepeatedNameIgnoringCase() {
        service.create(exercise("Sentadilla", MuscleGroup.LEGS));
        assertThrows(BusinessRuleException.class,
                () -> service.create(exercise("  sentadilla ", MuscleGroup.LEGS)));
    }

    @Test
    void createRejectsInvalidData() {
        assertThrows(InvalidDataException.class,
                () -> service.create(exercise("ab", MuscleGroup.CHEST)));   // nombre muy corto
    }

    @Test
    void updateKeepsTheSameNameForTheSameExercise() {
        Exercise saved = service.create(exercise("Remo", MuscleGroup.BACK));
        // Guardar el mismo ejercicio con su propio nombre no es un duplicado.
        Exercise updated = service.update(saved);
        assertEquals("Remo", updated.getName());
    }

    @Test
    void findByIdFailsWhenMissing() {
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99));
    }

    @Test
    void searchFiltersByTextAndMuscleGroup() {
        service.create(exercise("Press de banca", MuscleGroup.CHEST));
        service.create(exercise("Press militar", MuscleGroup.SHOULDERS));
        service.create(exercise("Sentadilla", MuscleGroup.LEGS));

        List<Exercise> byText = service.search("press", null, null);
        List<Exercise> byGroup = service.search("press", MuscleGroup.SHOULDERS, null);

        assertEquals(2, byText.size());
        assertEquals(1, byGroup.size());
        assertTrue(byGroup.get(0).getName().contains("militar"));
    }
}