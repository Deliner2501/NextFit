package com.nextfit.service.suggest;

import java.util.List;

/**
 * A routine proposed by someone that is not trusted yet, for example the AI. It only holds what was
 * proposed (ids and numbers), and nothing in it is true until {@link RoutineDraftChecker} has checked
 * it against the catalog of exercises and the limits of the member.
 *
 * @param days the proposed days, in order
 */
public record RoutineDraft(List<Day> days) {

    /**
     * One proposed day.
     *
     * @param name  the name of the day, as it was proposed
     * @param lines the proposed exercises, in the order they are done
     */
    public record Day(String name, List<Line> lines) {

        public Day {
            // EXPLICACIÓN: se copia la lista para que quede inmutable, como en Routine
            lines = lines == null ? List.of() : List.copyOf(lines);
        }
    }

    /**
     * One proposed exercise inside a day.
     *
     * @param exerciseId  the id of the exercise in the catalog
     * @param sets        the number of sets
     * @param repetitions the repetitions of each set
     * @param restSeconds the rest between sets, in seconds
     */
    public record Line(long exerciseId, int sets, int repetitions, int restSeconds) {
    }

    public RoutineDraft {
        days = days == null ? List.of() : List.copyOf(days);
    }
}