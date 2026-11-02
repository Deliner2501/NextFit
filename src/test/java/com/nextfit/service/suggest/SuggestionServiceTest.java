package com.nextfit.service.suggest;

import com.nextfit.model.BmiCategory;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.Measurement;
import com.nextfit.model.Member;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.TrainingGoal;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.persistence.CsvHealthProfileRepository;
import com.nextfit.persistence.CsvMeasurementRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MemberService;
import com.nextfit.validation.ExerciseValidator;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the routine suggestion, using real CSV files in a temporary folder. */
class SuggestionServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);

    private ExerciseService exercises;
    private SuggestionService service;
    private SuggestionService emptyCatalogService;
    private MemberService members;
    private HealthService health;

    // @TempDir da una carpeta nueva en cada prueba. Se llena el catálogo con tres
    // ejercicios (uno por dificultad) de cada grupo muscular.
    @BeforeEach
    void setUp(@TempDir Path folder) {
        exercises = new ExerciseService(
                new CsvExerciseRepository(folder.resolve("exercises.csv")), new ExerciseValidator());
        for (MuscleGroup group : MuscleGroup.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                exercises.create(new Exercise(0, group.getLabel() + " " + difficulty.getLabel(),
                        "Descripción", group, difficulty, ""));
            }
        }
        members = new MemberService(
                new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        health = new HealthService(members,
                new CsvHealthProfileRepository(folder.resolve("health.csv")),
                new CsvMeasurementRepository(folder.resolve("measurements.csv")),
                new HealthValidator());
        List<SuggestionRule> rules = List.of(new BmiRule(), new MedicalConditionRule());
        service = new SuggestionService(exercises, health, rules, CLOCK);
        emptyCatalogService = new SuggestionService(new ExerciseService(
                new CsvExerciseRepository(folder.resolve("empty.csv")), new ExerciseValidator()),
                health, rules, CLOCK);
    }

    private SuggestionRequest request(TrainingGoal goal, Difficulty level, int days) {
        return new SuggestionRequest(goal, level, days, null, "");
    }

    private Member newMember() {
        return members.register(new Member(0, "Ana Pérez", "1065123456", "3001234567",
                "ana@example.com", LocalDate.of(2000, 5, 10), LocalDate.of(2026, 10, 1), "",
                "Mamá 3110000000", true));
    }

    @Test
    void theRoutineHasOneDayForEachTrainingDay() {
        Routine routine = service.suggest(request(TrainingGoal.TONING, Difficulty.INTERMEDIATE, 4))
                .routine();

        assertEquals(4, routine.getDays().size());
        assertEquals("Día 1 · Tren superior", routine.getDays().get(0).getName());
    }

    @Test
    void everyExerciseUsesTheSetsRepetitionsAndRestOfTheGoal() {
        Routine routine = service.suggest(request(TrainingGoal.MUSCLE_GAIN, Difficulty.INTERMEDIATE, 3))
                .routine();

        for (RoutineDay day : routine.getDays()) {
            for (RoutineExercise line : day.getExercises()) {
                assertEquals(4, line.getSets());
                assertEquals(8, line.getRepetitions());
                assertEquals(90, line.getRestSeconds());
            }
        }
    }

    @Test
    void aBeginnerOnlyGetsBeginnerExercisesAndAtMostFivePerDay() {
        Routine routine = service.suggest(request(TrainingGoal.TONING, Difficulty.BEGINNER, 5)).routine();

        for (RoutineDay day : routine.getDays()) {
            assertTrue(day.getExercises().size() <= 5);
            for (RoutineExercise line : day.getExercises()) {
                assertEquals(Difficulty.BEGINNER, exercises.findById(line.getExerciseId()).getDifficulty());
            }
        }
    }

    @Test
    void noExerciseIsRepeatedInTheSameDay() {
        Routine routine = service.suggest(request(TrainingGoal.MUSCLE_GAIN, Difficulty.ADVANCED, 5))
                .routine();

        for (RoutineDay day : routine.getDays()) {
            Set<Long> ids = new HashSet<>();
            for (RoutineExercise line : day.getExercises()) {
                assertTrue(ids.add(line.getExerciseId()));
            }
        }
    }

    @Test
    void weightLossAddsOneCardioExerciseEveryDay() {
        Routine routine = service.suggest(request(TrainingGoal.WEIGHT_LOSS, Difficulty.INTERMEDIATE, 3))
                .routine();

        for (RoutineDay day : routine.getDays()) {
            long cardio = day.getExercises().stream()
                    .filter(line -> exercises.findById(line.getExerciseId()).getMuscleGroup() == MuscleGroup.CARDIO)
                    .count();
            assertEquals(1, cardio);
        }
    }

    @Test
    void theSuggestionIsADraftWithoutIdOrTrainer() {
        SuggestedRoutine result = service.suggest(request(TrainingGoal.TONING, Difficulty.BEGINNER, 2));

        assertEquals(0, result.routine().getId());
        assertEquals(0, result.routine().getTrainerId());
        assertEquals(LocalDate.of(2026, 11, 1), result.routine().getCreatedAt());
        assertEquals("Tonificar", result.routine().getObjective());
    }

    @Test
    void theRulesAdjustTheSuggestionAndLeaveNotes() {
        SuggestionRequest request = new SuggestionRequest(TrainingGoal.TONING, Difficulty.ADVANCED, 3,
                BmiCategory.OBESITY, "hipertensión");
        SuggestedRoutine result = service.suggest(request);

        for (RoutineDay day : result.routine().getDays()) {
            for (RoutineExercise line : day.getExercises()) {
                Exercise exercise = exercises.findById(line.getExerciseId());
                assertFalse(exercise.getDifficulty() == Difficulty.ADVANCED);
                if (exercise.getMuscleGroup() != MuscleGroup.CARDIO) {
                    assertEquals(90, line.getRestSeconds());
                }
            }
        }
        assertFalse(result.notes().isEmpty());
    }

    @Test
    void aMissingCatalogProducesNotesInsteadOfFailing() {
        SuggestedRoutine result = emptyCatalogService.suggest(
                request(TrainingGoal.WEIGHT_LOSS, Difficulty.BEGINNER, 2));

        assertEquals(2, result.routine().getDays().size());
        assertTrue(result.routine().getDays().get(0).getExercises().isEmpty());
        assertFalse(result.notes().isEmpty());
    }

    @Test
    void theRequestReadsTheBmiAndTheMedicalConditionsOfTheMember() {
        Member member = newMember();
        health.saveProfile(new HealthProfile(member.getId(), 170, "O+", "Asma y dolor de rodilla", ""));
        health.addMeasurement(new Measurement(0, member.getId(), LocalDate.of(2026, 10, 20), 95, 100, 105, 110));

        SuggestionRequest request = service.requestFor(member.getId(), TrainingGoal.WEIGHT_LOSS,
                Difficulty.BEGINNER, 3);

        assertEquals(BmiCategory.OBESITY, request.bmiCategory());
        assertEquals("Asma y dolor de rodilla", request.medicalConditions());
    }

    @Test
    void aMemberWithoutHealthDataGetsAnEmptyBmiAndConditions() {
        Member member = newMember();

        SuggestionRequest request = service.requestFor(member.getId(), TrainingGoal.TONING,
                Difficulty.BEGINNER, 2);

        assertNull(request.bmiCategory());
        assertEquals("", request.medicalConditions());
    }
}