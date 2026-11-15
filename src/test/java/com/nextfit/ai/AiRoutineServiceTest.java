package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.TrainingGoal;
import com.nextfit.persistence.CsvExerciseRepository;
import com.nextfit.persistence.CsvHealthProfileRepository;
import com.nextfit.persistence.CsvMeasurementRepository;
import com.nextfit.persistence.CsvMemberRepository;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.HealthService;
import com.nextfit.service.MemberService;
import com.nextfit.service.suggest.BmiRule;
import com.nextfit.service.suggest.MedicalConditionRule;
import com.nextfit.service.suggest.RoutineDraftChecker;
import com.nextfit.service.suggest.SuggestedRoutine;
import com.nextfit.service.suggest.SuggestionRequest;
import com.nextfit.service.suggest.SuggestionRule;
import com.nextfit.service.suggest.SuggestionService;
import com.nextfit.validation.ExerciseValidator;
import com.nextfit.validation.HealthValidator;
import com.nextfit.validation.MemberValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the routine that the AI proposes, with a fake AI and real CSV files in a temporary folder. */
class AiRoutineServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    private ExerciseService exercises;
    private SuggestionService suggestions;
    private Path folder;
    private final List<String> systems = new ArrayList<>();
    private final List<String> prompts = new ArrayList<>();

    @BeforeEach
    void setUp(@TempDir Path folder) {
        this.folder = folder;
        exercises = new ExerciseService(
                new CsvExerciseRepository(folder.resolve("exercises.csv")), new ExerciseValidator());
        for (MuscleGroup group : MuscleGroup.values()) {
            for (Difficulty difficulty : Difficulty.values()) {
                exercises.create(new Exercise(0, group.getLabel() + " " + difficulty.getLabel(),
                        "Descripción", group, difficulty, ""));
            }
        }
        MemberService members = new MemberService(
                new CsvMemberRepository(folder.resolve("members.csv")), new MemberValidator());
        HealthService health = new HealthService(members,
                new CsvHealthProfileRepository(folder.resolve("health.csv")),
                new CsvMeasurementRepository(folder.resolve("measurements.csv")),
                new HealthValidator());
        suggestions = new SuggestionService(exercises, health,
                List.<SuggestionRule>of(new BmiRule(), new MedicalConditionRule()), CLOCK);
    }

    private AiRoutineService serviceThatAnswers(String answer) {
        return new AiRoutineService((system, prompt, maxTokens) -> {
            systems.add(system);
            prompts.add(prompt);
            return answer;
        }, suggestions, exercises, new RoutineDraftChecker(CLOCK));
    }

    private long idOf(String name) {
        return exercises.findAll().stream().filter(e -> e.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    private static SuggestionRequest request(TrainingGoal goal, Difficulty level, int days, String conditions) {
        return new SuggestionRequest(goal, level, days, null, conditions);
    }

    private static String line(long id) {
        return "{\"id\":" + id + ",\"sets\":4,\"reps\":8,\"rest\":90}";
    }

    private String twoDays() {
        return "{\"days\":[{\"name\":\"Empuje\",\"exercises\":[" + line(idOf("Pecho Principiante")) + ","
                + line(idOf("Hombros Principiante")) + "]},{\"name\":\"Tirón\",\"exercises\":["
                + line(idOf("Espalda Principiante")) + "]}]}";
    }

    @Test
    void buildsTheRoutineFromTheJsonOfTheAi() {
        SuggestedRoutine result = serviceThatAnswers(twoDays())
                .propose(request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, ""));
        Routine routine = result.routine();

        assertEquals(2, routine.getDays().size());
        assertEquals("Empuje", routine.getDays().get(0).getName());
        assertEquals(2, routine.getDays().get(0).getExercises().size());
        assertEquals(idOf("Pecho Principiante"), routine.getDays().get(0).getExercises().get(0).getExerciseId());
        assertEquals(0, routine.getTrainerId());
        assertTrue(result.notes().get(0).contains("propuesta por la IA"));
    }

    @Test
    void anAnswerWrappedInMarkdownIsAccepted() {
        String wrapped = "Claro, aquí está:\n```json\n" + twoDays() + "\n```\nÉxitos";

        SuggestedRoutine result = serviceThatAnswers(wrapped)
                .propose(request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, ""));

        assertEquals(2, result.routine().getDays().size());
    }

    @Test
    void theAiOnlySeesTheGoalTheLevelAndTheAllowedExercises() {
        serviceThatAnswers(twoDays()).propose(request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, ""));

        String prompt = prompts.get(0);
        assertTrue(prompt.contains("Ganar músculo"));
        assertTrue(prompt.contains("Principiante"));
        assertTrue(prompt.contains("Pecho Principiante"));
        assertFalse(prompt.contains("Pecho Avanzado"));
        assertTrue(systems.get(0).contains("exactamente 2 días"));
    }

    @Test
    void theMedicalConditionsAreNeverSentButTheirLimitsAre() {
        serviceThatAnswers(twoDays()).propose(
                request(TrainingGoal.WEIGHT_LOSS, Difficulty.ADVANCED, 2, "diabetes y dolor de rodilla, hipertensión"));

        String sent = systems.get(0) + prompts.get(0);
        assertFalse(sent.toLowerCase().contains("diabetes"));
        assertFalse(sent.toLowerCase().contains("rodilla"));
        assertFalse(sent.toLowerCase().contains("hipertensi"));
        // EXPLICACIÓN: no se envía la condición, pero sí sus efectos: sin avanzados y descanso mínimo de 90 s
        assertFalse(prompts.get(0).contains("Piernas Avanzado"));
        assertTrue(prompts.get(0).contains("descanso: 90 segundos"));
    }

    @Test
    void anExerciseThatIsNotAllowedInTheAnswerIsRemoved() {
        String answer = "{\"days\":[{\"name\":\"A\",\"exercises\":[" + line(idOf("Pecho Principiante")) + ","
                + line(idOf("Piernas Avanzado")) + "," + line(9999) + "]},{\"name\":\"B\",\"exercises\":["
                + line(idOf("Espalda Principiante")) + "]}]}";

        SuggestedRoutine result = serviceThatAnswers(answer)
                .propose(request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, ""));

        assertEquals(1, result.routine().getDays().get(0).getExercises().size());
        assertTrue(result.notes().stream().anyMatch(note -> note.contains("Piernas Avanzado")));
        assertTrue(result.notes().stream().anyMatch(note -> note.contains("no existe en el catálogo")));
    }

    @Test
    void anAnswerThatCannotBeReadIsAnAiError() {
        SuggestionRequest request = request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, "");

        for (String answer : List.of("Lo siento, no puedo ayudarte", "{\"days\":5}", "{\"days\":[5]}",
                "{\"days\":[{\"name\":\"A\",\"exercises\":\"nada\"}]}",
                "{\"days\":[{\"name\":\"A\",\"exercises\":[{\"id\":\"x\",\"sets\":4,\"reps\":8,\"rest\":90}]}]}",
                "{\"days\":[{\"name\":\"A\",\"exercises\":[")) {
            AiException error = assertThrows(AiException.class, () -> serviceThatAnswers(answer).propose(request));
            assertTrue(error.getMessage().contains("formato esperado"));
        }
    }

    @Test
    void aWrongNumberOfDaysIsRefused() {
        SuggestionRequest request = request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 4, "");

        assertThrows(BusinessRuleException.class, () -> serviceThatAnswers(twoDays()).propose(request));
    }

    @Test
    void aFailureOfTheAiGoesUp() {
        AiRoutineService service = new AiRoutineService((system, prompt, maxTokens) -> {
            throw new AiException("El asistente está saturado");
        }, suggestions, exercises, new RoutineDraftChecker(CLOCK));

        AiException error = assertThrows(AiException.class,
                () -> service.propose(request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, "")));

        assertEquals("El asistente está saturado", error.getMessage());
    }

    @Test
    void withoutExercisesTheAiIsNotAsked() {
        ExerciseService empty = new ExerciseService(
                new CsvExerciseRepository(folder.resolve("empty.csv")), new ExerciseValidator());
        SuggestionService emptySuggestions = new SuggestionService(empty, null,
                List.<SuggestionRule>of(), CLOCK);
        AiRoutineService service = new AiRoutineService((system, prompt, maxTokens) -> {
            throw new AssertionError("the AI must not be asked");
        }, emptySuggestions, empty, new RoutineDraftChecker(CLOCK));

        assertThrows(BusinessRuleException.class,
                () -> service.propose(request(TrainingGoal.MUSCLE_GAIN, Difficulty.BEGINNER, 2, "")));
    }
}