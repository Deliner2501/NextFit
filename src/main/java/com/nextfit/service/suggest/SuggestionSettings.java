package com.nextfit.service.suggest;

import com.nextfit.model.Difficulty;
import com.nextfit.model.Exercise;
import com.nextfit.model.MuscleGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The adjustable settings used to build a suggested routine: the highest difficulty
 * allowed, the rest between sets, whether cardio is added every day, how many exercises
 * fit in a day and the extra limits per muscle group. It is immutable: every change
 * returns a new copy, so each rule hands its result to the next one.
 *
 * @param maxLevel        highest difficulty that may be suggested
 * @param restSeconds     rest between sets
 * @param cardioEveryDay  true when every day must include a cardio exercise
 * @param exercisesPerDay maximum number of exercises in a day
 * @param groupCaps       highest difficulty allowed for specific muscle groups
 * @param notes           warnings that explain the adjustments made
 */
public record SuggestionSettings(Difficulty maxLevel, int restSeconds, boolean cardioEveryDay,
                                 int exercisesPerDay, Map<MuscleGroup, Difficulty> groupCaps,
                                 List<String> notes) {

    private static final int BEGINNER_EXERCISES_PER_DAY = 5;
    private static final int REGULAR_EXERCISES_PER_DAY = 6;

    // EXPLICACIÓN: se copian el mapa y la lista para que nadie desde afuera pueda
    // modificarlos. Es el mismo cuidado que se tuvo con List.copyOf en Routine.
    public SuggestionSettings {
        groupCaps = Map.copyOf(groupCaps);
        notes = List.copyOf(notes);
    }

    /**
     * Builds the starting settings from the request, before any rule adjusts them.
     * The goal gives the rest and the cardio, and the level gives the limits.
     *
     * @param request what the trainer asked for
     * @return the starting settings
     */
    public static SuggestionSettings from(SuggestionRequest request) {
        int perDay = request.level() == Difficulty.BEGINNER
                ? BEGINNER_EXERCISES_PER_DAY : REGULAR_EXERCISES_PER_DAY;
        return new SuggestionSettings(request.level(), request.goal().getRestSeconds(),
                request.goal().requiresCardioEveryDay(), perDay, Map.of(), List.of());
    }

    /**
     * Tells whether an exercise can be suggested with these settings.
     *
     * @param exercise the exercise to check
     * @return true when its difficulty is within the general limit and the limit of its group
     */
    public boolean allows(Exercise exercise) {
        // EXPLICACIÓN: compareTo en un enum compara por el orden en que se
        // declararon: BEGINNER < INTERMEDIATE < ADVANCED.
        if (exercise.getDifficulty().compareTo(maxLevel) > 0) {
            return false;
        }
        Difficulty groupLimit = groupCaps.get(exercise.getMuscleGroup());
        return groupLimit == null || exercise.getDifficulty().compareTo(groupLimit) <= 0;
    }

    /**
     * Lowers the general difficulty limit; a higher limit than the current one is ignored.
     *
     * @param limit the new highest difficulty
     * @return a copy with the limit applied
     */
    public SuggestionSettings withMaxLevelAtMost(Difficulty limit) {
        Difficulty lower = limit.compareTo(maxLevel) < 0 ? limit : maxLevel;
        return new SuggestionSettings(lower, restSeconds, cardioEveryDay,
                exercisesPerDay, groupCaps, notes);
    }

    /**
     * Raises the rest between sets; a shorter rest than the current one is ignored.
     *
     * @param seconds the minimum rest wanted
     * @return a copy with the rest applied
     */
    public SuggestionSettings withRestAtLeast(int seconds) {
        return new SuggestionSettings(maxLevel, Math.max(restSeconds, seconds), cardioEveryDay,
                exercisesPerDay, groupCaps, notes);
    }

    /**
     * Makes every day include a cardio exercise.
     *
     * @return a copy with cardio every day
     */
    public SuggestionSettings withCardioEveryDay() {
        return new SuggestionSettings(maxLevel, restSeconds, true,
                exercisesPerDay, groupCaps, notes);
    }

    /**
     * Removes the cardio exercise that the goal added every day.
     *
     * @return a copy without cardio every day
     */
    public SuggestionSettings withoutCardioEveryDay() {
        return new SuggestionSettings(maxLevel, restSeconds, false,
                exercisesPerDay, groupCaps, notes);
    }

    /**
     * Limits the difficulty for one muscle group; if the group already has a lower
     * limit, that one stays.
     *
     * @param group the muscle group to limit
     * @param limit the highest difficulty allowed for that group
     * @return a copy with the limit applied
     */
    public SuggestionSettings withGroupCap(MuscleGroup group, Difficulty limit) {
        Map<MuscleGroup, Difficulty> updated = new HashMap<>(groupCaps);
        Difficulty current = updated.get(group);
        if (current == null || limit.compareTo(current) < 0) {
            updated.put(group, limit);
        }
        return new SuggestionSettings(maxLevel, restSeconds, cardioEveryDay,
                exercisesPerDay, updated, notes);
    }

    /**
     * Adds a warning that explains an adjustment; repeated texts are ignored.
     *
     * @param note the text to show to the trainer
     * @return a copy with the note added
     */
    public SuggestionSettings withNote(String note) {
        if (notes.contains(note)) {
            return this;
        }
        List<String> updated = new ArrayList<>(notes);
        updated.add(note);
        return new SuggestionSettings(maxLevel, restSeconds, cardioEveryDay,
                exercisesPerDay, groupCaps, updated);
    }
}