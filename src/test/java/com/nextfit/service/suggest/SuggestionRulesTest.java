package com.nextfit.service.suggest;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.BmiCategory;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.TrainingGoal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the request, the settings and the two suggestion rules. */
class SuggestionRulesTest {

    private final BmiRule bmiRule = new BmiRule();
    private final MedicalConditionRule medicalRule = new MedicalConditionRule();

    // Crea una solicitud de prueba con los datos que cambian de una prueba a otra.
    private SuggestionRequest request(TrainingGoal goal, Difficulty level,
                                      BmiCategory bmi, String conditions) {
        return new SuggestionRequest(goal, level, 3, bmi, conditions);
    }

    // Crea un ejercicio de prueba con el grupo y la dificultad indicados.
    private Exercise exercise(MuscleGroup group, Difficulty difficulty) {
        return new Exercise(1, "Ejercicio", "Descripción", group, difficulty, "");
    }

    @Test
    void requestRejectsDaysOutsideTheRange() {
        assertThrows(BusinessRuleException.class,
                () -> new SuggestionRequest(TrainingGoal.TONING, Difficulty.BEGINNER, 1, null, ""));
        assertThrows(BusinessRuleException.class,
                () -> new SuggestionRequest(TrainingGoal.TONING, Difficulty.BEGINNER, 6, null, ""));
    }

    @Test
    void requestRequiresGoalAndLevel() {
        assertThrows(BusinessRuleException.class,
                () -> new SuggestionRequest(null, Difficulty.BEGINNER, 3, null, ""));
        assertThrows(BusinessRuleException.class,
                () -> new SuggestionRequest(TrainingGoal.TONING, null, 3, null, ""));
    }

    @Test
    void startingSettingsComeFromTheGoalAndTheLevel() {
        SuggestionSettings beginner = SuggestionSettings.from(
                request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, null, ""));
        SuggestionSettings advanced = SuggestionSettings.from(
                request(TrainingGoal.WEIGHT_LOSS, Difficulty.ADVANCED, null, ""));

        assertEquals(90, beginner.restSeconds());
        assertEquals(5, beginner.exercisesPerDay());
        assertFalse(beginner.cardioEveryDay());
        assertEquals(45, advanced.restSeconds());
        assertEquals(6, advanced.exercisesPerDay());
        assertTrue(advanced.cardioEveryDay());
    }

    @Test
    void settingsOnlyAllowExercisesWithinTheLevel() {
        SuggestionSettings settings = SuggestionSettings.from(
                request(TrainingGoal.TONING, Difficulty.INTERMEDIATE, null, ""));

        assertTrue(settings.allows(exercise(MuscleGroup.CHEST, Difficulty.BEGINNER)));
        assertTrue(settings.allows(exercise(MuscleGroup.CHEST, Difficulty.INTERMEDIATE)));
        assertFalse(settings.allows(exercise(MuscleGroup.CHEST, Difficulty.ADVANCED)));
    }

    @Test
    void obesityAddsCardioAndLimitsTheLevel() {
        SuggestionRequest request = request(TrainingGoal.TONING, Difficulty.ADVANCED,
                BmiCategory.OBESITY, "");
        SuggestionSettings result = bmiRule.apply(SuggestionSettings.from(request), request);

        assertTrue(result.cardioEveryDay());
        assertEquals(Difficulty.INTERMEDIATE, result.maxLevel());
        assertEquals(1, result.notes().size());
    }

    @Test
    void overweightOnlyAddsCardio() {
        SuggestionRequest request = request(TrainingGoal.TONING, Difficulty.ADVANCED,
                BmiCategory.OVERWEIGHT, "");
        SuggestionSettings result = bmiRule.apply(SuggestionSettings.from(request), request);

        assertTrue(result.cardioEveryDay());
        assertEquals(Difficulty.ADVANCED, result.maxLevel());
    }

    @Test
    void underweightRemovesTheCardioOfTheGoal() {
        SuggestionRequest request = request(TrainingGoal.WEIGHT_LOSS, Difficulty.BEGINNER,
                BmiCategory.UNDERWEIGHT, "");
        SuggestionSettings result = bmiRule.apply(SuggestionSettings.from(request), request);

        assertFalse(result.cardioEveryDay());
    }

    @Test
    void unknownOrNormalBmiChangesNothing() {
        SuggestionRequest unknown = request(TrainingGoal.TONING, Difficulty.BEGINNER, null, "");
        SuggestionRequest normal = request(TrainingGoal.TONING, Difficulty.BEGINNER,
                BmiCategory.NORMAL, "");

        assertEquals(SuggestionSettings.from(unknown),
                bmiRule.apply(SuggestionSettings.from(unknown), unknown));
        assertEquals(SuggestionSettings.from(normal),
                bmiRule.apply(SuggestionSettings.from(normal), normal));
    }

    @Test
    void kneeProblemsLimitAdvancedLegExercisesOnly() {
        SuggestionRequest request = request(TrainingGoal.TONING, Difficulty.ADVANCED,
                null, "Dolor de RODILLA derecha");
        SuggestionSettings result = medicalRule.apply(SuggestionSettings.from(request), request);

        assertFalse(result.allows(exercise(MuscleGroup.LEGS, Difficulty.ADVANCED)));
        assertTrue(result.allows(exercise(MuscleGroup.LEGS, Difficulty.INTERMEDIATE)));
        assertTrue(result.allows(exercise(MuscleGroup.CHEST, Difficulty.ADVANCED)));
    }

    @Test
    void backProblemsLimitAdvancedBackExercises() {
        SuggestionRequest request = request(TrainingGoal.TONING, Difficulty.ADVANCED,
                null, "Hernia discal");
        SuggestionSettings result = medicalRule.apply(SuggestionSettings.from(request), request);

        assertFalse(result.allows(exercise(MuscleGroup.BACK, Difficulty.ADVANCED)));
        assertTrue(result.allows(exercise(MuscleGroup.LEGS, Difficulty.ADVANCED)));
    }

    @Test
    void heartProblemsLengthenTheRestAndRemoveAdvancedExercises() {
        SuggestionRequest request = request(TrainingGoal.WEIGHT_LOSS, Difficulty.ADVANCED,
                null, "Hipertensión");
        SuggestionSettings result = medicalRule.apply(SuggestionSettings.from(request), request);

        assertEquals(90, result.restSeconds());
        assertEquals(Difficulty.INTERMEDIATE, result.maxLevel());
    }

    @Test
    void emptyConditionsChangeNothing() {
        SuggestionRequest request = request(TrainingGoal.TONING, Difficulty.BEGINNER, null, null);

        assertEquals(SuggestionSettings.from(request),
                medicalRule.apply(SuggestionSettings.from(request), request));
    }

    @Test
    void severalConditionsAreAppliedTogether() {
        SuggestionRequest request = request(TrainingGoal.TONING, Difficulty.ADVANCED,
                BmiCategory.OBESITY, "rodilla y corazón");
        SuggestionSettings settings = SuggestionSettings.from(request);
        settings = bmiRule.apply(settings, request);
        settings = medicalRule.apply(settings, request);

        assertEquals(Difficulty.INTERMEDIATE, settings.maxLevel());
        assertEquals(90, settings.restSeconds());
        assertTrue(settings.cardioEveryDay());
        assertEquals(3, settings.notes().size());
    }
}