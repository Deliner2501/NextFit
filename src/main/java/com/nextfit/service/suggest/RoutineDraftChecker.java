package com.nextfit.service.suggest;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a {@link RoutineDraft} (what the AI proposed) into a routine that is safe to show to a trainer.
 * The AI is never trusted: every exercise must exist in the catalog and be allowed by the same limits
 * the suggestion rules give (level, medical conditions), the numbers must be reasonable, and anything
 * that does not pass is removed with a note. A draft that cannot be fixed is refused. The result is
 * only a suggestion: it is never assigned to a member, the trainer decides.
 */
public final class RoutineDraftChecker {

    /** Most sets of one exercise. */
    public static final int MAX_SETS = 6;
    /** Most repetitions of one set. */
    public static final int MAX_REPETITIONS = 30;
    /** Longest rest between sets, in seconds. */
    public static final int MAX_REST_SECONDS = 300;
    /** Longest name of a day; a longer one is cut. */
    public static final int MAX_DAY_NAME = 40;

    private static final String AI_NOTE = "Rutina propuesta por la IA: revísala y ajústala antes de asignarla.";
    private static final String UNUSABLE = "La IA no entregó una rutina que se pueda usar. Intenta de nuevo";

    private final Clock clock;

    /**
     * Creates the checker.
     *
     * @param clock gives the creation date of the routine
     */
    public RoutineDraftChecker(Clock clock) {
        this.clock = clock;
    }

    /**
     * Checks a draft and builds the routine.
     *
     * @param draft    what the AI proposed
     * @param request  what was asked (goal, level and days per week)
     * @param settings the limits of the member, after the suggestion rules were applied
     * @param catalog  the exercises that exist
     * @return the routine (id 0, no trainer) and the notes for the trainer
     * @throws BusinessRuleException when the draft has not the days that were asked or a day is left empty
     */
    public SuggestedRoutine check(RoutineDraft draft, SuggestionRequest request, SuggestionSettings settings,
                                  Collection<Exercise> catalog) {
        if (draft == null || draft.days().size() != request.daysPerWeek()) {
            throw new BusinessRuleException(UNUSABLE);
        }
        Map<Long, Exercise> byId = new HashMap<>();
        for (Exercise exercise : catalog) {
            byId.put(exercise.getId(), exercise);
        }
        Set<String> notes = new LinkedHashSet<>();
        notes.add(AI_NOTE);
        notes.addAll(settings.notes());

        List<RoutineDay> days = new ArrayList<>();
        for (int i = 0; i < draft.days().size(); i++) {
            days.add(checkDay(draft.days().get(i), i + 1, settings, byId, notes));
        }
        Routine routine = new Routine(0,
                "IA · " + request.goal().getLabel() + " · " + request.daysPerWeek() + " días",
                request.goal().getLabel(), 0, LocalDate.now(clock), days);
        return new SuggestedRoutine(routine, new ArrayList<>(notes));
    }

    private static RoutineDay checkDay(RoutineDraft.Day day, int number, SuggestionSettings settings,
                                       Map<Long, Exercise> byId, Set<String> notes) {
        String name = cleanName(day.name(), number);
        // EXPLICACIÓN: se deja un lugar más que el máximo de la regla porque el cardio se suma aparte
        int maxLines = settings.exercisesPerDay() + 1;
        Set<Long> seen = new HashSet<>();
        List<RoutineExercise> lines = new ArrayList<>();
        for (RoutineDraft.Line line : day.lines()) {
            Exercise exercise = byId.get(line.exerciseId());
            if (exercise == null) {
                notes.add("Se quitó un ejercicio que no existe en el catálogo.");
            } else if (!settings.allows(exercise)) {
                notes.add("Se quitó «" + exercise.getName() + "»: no es adecuado para este nivel o estas condiciones.");
            } else if (!seen.add(exercise.getId())) {
                notes.add("Se quitó «" + exercise.getName() + "» porque estaba repetido en «" + name + "».");
            } else if (outOfRange(line)) {
                notes.add("Se quitó «" + exercise.getName() + "»: series, repeticiones o descanso fuera de lo normal.");
            } else if (lines.size() >= maxLines) {
                notes.add("Se quitaron ejercicios de más en «" + name + "».");
            } else {
                lines.add(new RoutineExercise(exercise.getId(), line.sets(), line.repetitions(),
                        restOf(line, exercise, settings)));
            }
        }
        if (lines.isEmpty()) {
            throw new BusinessRuleException(UNUSABLE);
        }
        return new RoutineDay(name, lines);
    }

    private static boolean outOfRange(RoutineDraft.Line line) {
        return line.sets() < 1 || line.sets() > MAX_SETS
                || line.repetitions() < 1 || line.repetitions() > MAX_REPETITIONS
                || line.restSeconds() < 0 || line.restSeconds() > MAX_REST_SECONDS;
    }

    // EXPLICACIÓN: si una condición médica pide un descanso mínimo, la IA no puede acortarlo; el cardio no tiene descanso
    private static int restOf(RoutineDraft.Line line, Exercise exercise, SuggestionSettings settings) {
        if (exercise.getMuscleGroup() == MuscleGroup.CARDIO) {
            return line.restSeconds();
        }
        return Math.max(line.restSeconds(), settings.restSeconds());
    }

    // EXPLICACIÓN: el nombre del día viene de la IA, así que es un dato: se limpia y se acorta, nunca se confía en él
    private static String cleanName(String name, int number) {
        String clean = name == null ? "" : name.replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) {
            return "Día " + number;
        }
        return clean.length() > MAX_DAY_NAME ? clean.substring(0, MAX_DAY_NAME).trim() : clean;
    }
}