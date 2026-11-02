package com.nextfit.service.suggest;

import com.nextfit.model.BmiCategory;
import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.HealthProfile;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.TrainingGoal;
import com.nextfit.service.ExerciseService;
import com.nextfit.service.HealthService;
import com.nextfit.service.suggest.WeeklySplit.DayTemplate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a suggested routine from the exercise catalog. It only coordinates: the rules
 * adjust the settings, the weekly split decides the muscle groups of each day, and this
 * class fills each slot with an exercise from the catalog. The result is a draft that the
 * trainer reviews; nothing is saved here.
 */
public class SuggestionService {

    private static final int CARDIO_SETS = 1;
    private static final int CARDIO_REPETITIONS = 1;
    private static final int CARDIO_REST_SECONDS = 0;
    private static final String CARDIO_NOTE =
            "Cardio: el entrenador define el tiempo de cada sesión (por ejemplo, 15 minutos).";

    private final ExerciseService exercises;
    private final HealthService health;
    private final List<SuggestionRule> rules;
    private final Clock clock;

    // EXPLICACIÓN: el servicio recibe todo por el constructor (inyección de
    // dependencias). Las reglas llegan como una lista de la interfaz
    // SuggestionRule: el servicio no sabe cuáles son, así que agregar una regla
    // nueva no lo obliga a cambiar (OCP y DIP). El Clock permite probar la
    // fecha de creación sin esperar a un día real.
    public SuggestionService(ExerciseService exercises, HealthService health,
                             List<SuggestionRule> rules, Clock clock) {
        this.exercises = exercises;
        this.health = health;
        this.rules = List.copyOf(rules);
        this.clock = clock;
    }

    /**
     * Builds the request for a member, reading its body mass index and medical conditions
     * from the health module. Missing data is not an error: the rules simply do not apply.
     *
     * @param memberId    the member to suggest a routine for
     * @param goal        what the member wants to reach
     * @param level       experience of the member
     * @param daysPerWeek training days per week, from 2 to 5
     * @return the request, ready for {@link #suggest(SuggestionRequest)}
     */
    public SuggestionRequest requestFor(long memberId, TrainingGoal goal, Difficulty level,
                                        int daysPerWeek) {
        BmiCategory bmi = health.currentBmi(memberId).map(health::classify).orElse(null);
        String conditions = health.findProfile(memberId)
                .map(HealthProfile::getMedicalConditions).orElse("");
        return new SuggestionRequest(goal, level, daysPerWeek, bmi, conditions);
    }

    /**
     * Builds a routine draft for the request.
     *
     * @param request what the trainer asked for
     * @return the routine draft and the notes that explain it
     */
    public SuggestedRoutine suggest(SuggestionRequest request) {
        SuggestionSettings settings = adjust(request);
        Map<MuscleGroup, List<Exercise>> byGroup = exercises.findAll().stream()
                .filter(settings::allows)
                .collect(Collectors.groupingBy(Exercise::getMuscleGroup));

        // EXPLICACIÓN: "usedOverall" recuerda los ejercicios ya puestos en cualquier
        // día, para que los días no repitan los mismos si el catálogo tiene más.
        Set<Long> usedOverall = new HashSet<>();
        Set<String> warnings = new LinkedHashSet<>(settings.notes());
        List<RoutineDay> days = new ArrayList<>();
        for (DayTemplate template : WeeklySplit.forDays(request.daysPerWeek())) {
            days.add(buildDay(template, settings, request.goal(), byGroup, usedOverall, warnings));
        }
        Routine routine = new Routine(0,
                "Sugerida · " + request.goal().getLabel() + " · " + request.daysPerWeek() + " días",
                request.goal().getLabel(), 0, LocalDate.now(clock), days);
        return new SuggestedRoutine(routine, new ArrayList<>(warnings));
    }

    private SuggestionSettings adjust(SuggestionRequest request) {
        SuggestionSettings settings = SuggestionSettings.from(request);
        for (SuggestionRule rule : rules) {
            settings = rule.apply(settings, request);
        }
        return settings;
    }

    private RoutineDay buildDay(DayTemplate template, SuggestionSettings settings,
                                TrainingGoal goal, Map<MuscleGroup, List<Exercise>> byGroup,
                                Set<Long> usedOverall, Set<String> warnings) {
        List<RoutineExercise> lines = new ArrayList<>();
        Set<Long> usedToday = new HashSet<>();
        int slotCount = Math.min(settings.exercisesPerDay(), template.slots().size());
        for (MuscleGroup group : template.slots().subList(0, slotCount)) {
            Optional<Exercise> pick = pick(byGroup.get(group), usedToday, usedOverall);
            if (pick.isEmpty()) {
                warnings.add("No hay suficientes ejercicios de "
                        + group.getLabel().toLowerCase() + " para este nivel y estas condiciones.");
                continue;
            }
            markUsed(pick.get(), usedToday, usedOverall);
            lines.add(new RoutineExercise(pick.get().getId(), goal.getSets(),
                    goal.getRepetitions(), settings.restSeconds()));
        }
        if (settings.cardioEveryDay()) {
            addCardio(lines, byGroup, usedToday, usedOverall, warnings);
        }
        if (lines.isEmpty()) {
            warnings.add("El día \"" + template.name() + "\" quedó sin ejercicios.");
        }
        return new RoutineDay(template.name(), lines);
    }

    private void addCardio(List<RoutineExercise> lines, Map<MuscleGroup, List<Exercise>> byGroup,
                           Set<Long> usedToday, Set<Long> usedOverall, Set<String> warnings) {
        Optional<Exercise> pick = pick(byGroup.get(MuscleGroup.CARDIO), usedToday, usedOverall);
        if (pick.isEmpty()) {
            warnings.add("No hay ejercicios de cardio disponibles para este nivel.");
            return;
        }
        markUsed(pick.get(), usedToday, usedOverall);
        lines.add(new RoutineExercise(pick.get().getId(), CARDIO_SETS,
                CARDIO_REPETITIONS, CARDIO_REST_SECONDS));
        warnings.add(CARDIO_NOTE);
    }

    private Optional<Exercise> pick(List<Exercise> candidates, Set<Long> usedToday,
                                    Set<Long> usedOverall) {
        if (candidates == null) {
            return Optional.empty();
        }
        // EXPLICACIÓN: primero se busca un ejercicio que no se haya usado en
        // ningún día; si no queda, se acepta uno repetido de otro día, pero
        // nunca dos veces el mismo ejercicio en el mismo día.
        for (Exercise candidate : candidates) {
            if (!usedOverall.contains(candidate.getId())) {
                return Optional.of(candidate);
            }
        }
        for (Exercise candidate : candidates) {
            if (!usedToday.contains(candidate.getId())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private void markUsed(Exercise exercise, Set<Long> usedToday, Set<Long> usedOverall) {
        usedToday.add(exercise.getId());
        usedOverall.add(exercise.getId());
    }
}