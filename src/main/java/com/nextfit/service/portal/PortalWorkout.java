package com.nextfit.service.portal;

import java.util.List;

/**
 * The day of the routine that the member should train next.
 *
 * @param routineName the name of the routine
 * @param dayName     the name of the day, as the trainer wrote it
 * @param dayNumber   the position of the day inside the routine, starting at 1
 * @param dayCount    how many days the routine has
 * @param exercises   the exercises of the day, in the order they are done
 */
public record PortalWorkout(String routineName, String dayName, int dayNumber, int dayCount,
                            List<PortalExercise> exercises) {
}
