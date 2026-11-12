package com.nextfit.service.portal;

import com.nextfit.model.Routine;
import com.nextfit.model.TrainingSession;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Decides which day of a routine the member trains next. The days are done in rotation: after the
 * last day logged comes the next one, and after the last day of the routine comes the first. A
 * member who has not logged any session yet starts with the first day.
 */
public final class WorkoutPlanner {

    private WorkoutPlanner() {
    }

    /**
     * Finds the day to train next.
     *
     * @param routine  the routine assigned to the member
     * @param sessions the sessions the member logged with that assignment, in any order
     * @return the position of the day inside the routine, starting at 0; -1 when the routine has no days
     */
    public static int nextDayIndex(Routine routine, List<TrainingSession> sessions) {
        int count = routine.getDays().size();
        if (count == 0) {
            return -1;
        }
        Optional<TrainingSession> last = sessions.stream()
                .max(Comparator.comparing(TrainingSession::getDate).thenComparing(TrainingSession::getId));
        if (last.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < count; i++) {
            if (routine.getDays().get(i).getName().equals(last.get().getDayName())) {
                return (i + 1) % count;
            }
        }
        return 0;
    }
}
