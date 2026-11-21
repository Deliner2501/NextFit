package com.nextfit.service.portal;

/**
 * One exercise of the workout of the day, with the name already looked up in the catalog.
 *
 * @param name        the name of the exercise
 * @param muscleGroup the muscle group, in Spanish
 * @param sets        the number of sets
 * @param repetitions the repetitions of each set
 * @param restSeconds the rest between sets, in seconds
 */
public record PortalExercise(String name, String muscleGroup, int sets, int repetitions, int restSeconds) {
}
