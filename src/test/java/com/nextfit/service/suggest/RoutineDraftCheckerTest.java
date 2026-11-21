package com.nextfit.service.suggest;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.TrainingGoal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineDraftCheckerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private static final Exercise BENCH = exercise(1, "Press de banca", MuscleGroup.CHEST, Difficulty.BEGINNER);
    private static final Exercise ROW = exercise(2, "Remo", MuscleGroup.BACK, Difficulty.BEGINNER);
    private static final Exercise SQUAT = exercise(3, "Sentadilla", MuscleGroup.LEGS, Difficulty.BEGINNER);
    private static final Exercise HARD_SQUAT = exercise(4, "Sentadilla búlgara", MuscleGroup.LEGS, Difficulty.ADVANCED);
    private static final Exercise TREADMILL = exercise(5, "Trotadora", MuscleGroup.CARDIO, Difficulty.BEGINNER);
    private static final Exercise CURL = exercise(6, "Curl", MuscleGroup.ARMS, Difficulty.BEGINNER);
    private static final Exercise PLANK = exercise(7, "Plancha", MuscleGroup.CORE, Difficulty.BEGINNER);
    private static final Exercise PRESS = exercise(8, "Press militar", MuscleGroup.SHOULDERS, Difficulty.BEGINNER);
    private static final Exercise BRIDGE = exercise(9, "Puente", MuscleGroup.GLUTES, Difficulty.BEGINNER);
    private static final List<Exercise> CATALOG =
            List.of(BENCH, ROW, SQUAT, HARD_SQUAT, TREADMILL, CURL, PLANK, PRESS, BRIDGE);

    private final RoutineDraftChecker checker = new RoutineDraftChecker(CLOCK);

    private static Exercise exercise(long id, String name, MuscleGroup group, Difficulty level) {
        return new Exercise(id, name, "", group, level, "");
    }

    private static SuggestionRequest request(int days) {
        return new SuggestionRequest(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, days, null, "");
    }

    private static RoutineDraft.Line line(long exerciseId) {
        return new RoutineDraft.Line(exerciseId, 4, 8, 90);
    }

    private static RoutineDraft.Day day(String name, RoutineDraft.Line... lines) {
        return new RoutineDraft.Day(name, List.of(lines));
    }

    private SuggestedRoutine check(RoutineDraft draft, int days) {
        SuggestionRequest request = request(days);
        return checker.check(draft, request, SuggestionSettings.from(request), CATALOG);
    }

    @Test
    void aValidDraftBecomesARoutineThatIsNotAssignedToAnybody() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("Empuje", line(1), line(8)), day("Tirón", line(2), line(6))));

        SuggestedRoutine result = check(draft, 2);
        Routine routine = result.routine();

        assertEquals(0, routine.getId());
        assertEquals(0, routine.getTrainerId());
        assertEquals("IA · Ganar músculo · 2 días", routine.getName());
        assertEquals(LocalDate.of(2026, 11, 15), routine.getCreatedAt());
        assertEquals(2, routine.getDays().size());
        assertEquals("Empuje", routine.getDays().get(0).getName());
        assertEquals(2, routine.getDays().get(0).getExercises().size());
        assertEquals(1, routine.getDays().get(0).getExercises().get(0).getExerciseId());
        assertTrue(result.notes().get(0).contains("propuesta por la IA"));
    }

    @Test
    void aDraftWithOtherNumberOfDaysIsRefused() {
        RoutineDraft draft = new RoutineDraft(List.of(day("Único", line(1))));

        assertThrows(BusinessRuleException.class, () -> check(draft, 3));
        assertThrows(BusinessRuleException.class, () -> check(null, 3));
        assertThrows(BusinessRuleException.class, () -> check(new RoutineDraft(null), 3));
    }

    @Test
    void anExerciseThatDoesNotExistIsRemovedWithANote() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", line(1), line(999)), day("B", line(2))));

        SuggestedRoutine result = check(draft, 2);

        assertEquals(1, result.routine().getDays().get(0).getExercises().size());
        assertTrue(result.notes().stream().anyMatch(note -> note.contains("no existe en el catálogo")));
    }

    @Test
    void anExerciseTooHardForTheLevelIsRemoved() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", line(3), line(4)), day("B", line(2))));

        SuggestedRoutine result = check(draft, 2);

        assertEquals(List.of(3L), ids(result.routine().getDays().get(0)));
        assertTrue(result.notes().stream().anyMatch(note -> note.contains("Sentadilla búlgara")));
    }

    @Test
    void theLimitsOfTheMedicalRulesAreRespected() {
        SuggestionRequest request = request(2);
        SuggestionSettings kneeSettings = SuggestionSettings.from(request)
                .withGroupCap(MuscleGroup.LEGS, Difficulty.BEGINNER)
                .withNote("Rodilla: se quitaron los ejercicios de piernas de nivel avanzado.");
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", line(3), line(4)), day("B", line(2))));

        SuggestedRoutine result = checker.check(draft, request, kneeSettings, CATALOG);

        assertEquals(List.of(3L), ids(result.routine().getDays().get(0)));
        assertTrue(result.notes().stream().anyMatch(note -> note.startsWith("Rodilla")));
    }

    @Test
    void aRepeatedExerciseInTheSameDayIsRemoved() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", line(1), line(1)), day("B", line(2))));

        SuggestedRoutine result = check(draft, 2);

        assertEquals(List.of(1L), ids(result.routine().getDays().get(0)));
        assertTrue(result.notes().stream().anyMatch(note -> note.contains("repetido")));
    }

    @Test
    void numbersOutOfRangeAreRemoved() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", new RoutineDraft.Line(1, 20, 8, 90), new RoutineDraft.Line(2, 4, 0, 90),
                        new RoutineDraft.Line(3, 4, 8, 900), new RoutineDraft.Line(6, 4, 8, 60)),
                day("B", line(2))));

        SuggestedRoutine result = check(draft, 2);

        assertEquals(List.of(6L), ids(result.routine().getDays().get(0)));
    }

    @Test
    void aDayWithTooManyExercisesIsTrimmed() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", line(1), line(2), line(3), line(5), line(6), line(7), line(8), line(9)),
                day("B", line(2))));

        SuggestedRoutine result = check(draft, 2);

        // EXPLICACIÓN: principiante = 5 ejercicios por día y un lugar más para el cardio
        assertEquals(6, result.routine().getDays().get(0).getExercises().size());
        assertTrue(result.notes().stream().anyMatch(note -> note.contains("de más")));
    }

    @Test
    void theRestMinimumOfTheMedicalRulesCannotBeShortened() {
        SuggestionRequest request = request(2);
        SuggestionSettings heartSettings = SuggestionSettings.from(request).withRestAtLeast(90);
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", new RoutineDraft.Line(1, 4, 8, 30), new RoutineDraft.Line(5, 1, 1, 0)),
                day("B", line(2))));

        Routine routine = checker.check(draft, request, heartSettings, CATALOG).routine();

        assertEquals(90, routine.getDays().get(0).getExercises().get(0).getRestSeconds());
        assertEquals(0, routine.getDays().get(0).getExercises().get(1).getRestSeconds());
    }

    @Test
    void aDayLeftWithoutExercisesRefusesTheWholeDraft() {
        RoutineDraft draft = new RoutineDraft(List.of(
                day("A", line(1)), day("B", line(999))));

        assertThrows(BusinessRuleException.class, () -> check(draft, 2));
    }

    @Test
    void theNameOfTheDayIsCleanedAndCut() {
        String longName = "  Día   de   pierna  " + "x".repeat(80);
        RoutineDraft draft = new RoutineDraft(List.of(
                day(null, line(1)), day("   ", line(2)), day(longName, line(3))));

        List<RoutineDay> days = check(draft, 3).routine().getDays();

        assertEquals("Día 1", days.get(0).getName());
        assertEquals("Día 2", days.get(1).getName());
        assertTrue(days.get(2).getName().length() <= RoutineDraftChecker.MAX_DAY_NAME);
        assertTrue(days.get(2).getName().startsWith("Día de pierna"));
    }

    private static List<Long> ids(RoutineDay day) {
        List<Long> ids = new ArrayList<>();
        for (RoutineExercise exercise : day.getExercises()) {
            ids.add(exercise.getExerciseId());
        }
        return ids;
    }
}