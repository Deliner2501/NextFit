package com.nextfit.service.portal;

import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import com.nextfit.model.TrainingSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkoutPlannerTest {

    private static Routine routine(String... dayNames) {
        List<RoutineDay> days = java.util.Arrays.stream(dayNames)
                .map(name -> new RoutineDay(name, List.of(new RoutineExercise(1, 3, 10, 60))))
                .toList();
        return new Routine(1, "Fuerza", "Ganar fuerza", 1, LocalDate.of(2026, 11, 1), days);
    }

    private static TrainingSession session(long id, String day, int dayOfMonth) {
        return new TrainingSession(id, 7, 1, day, LocalDate.of(2026, 11, dayOfMonth), 60, "");
    }

    @Test
    void startsWithTheFirstDayWhenNothingWasLogged() {
        assertEquals(0, WorkoutPlanner.nextDayIndex(routine("Pecho", "Espalda", "Pierna"), List.of()));
    }

    @Test
    void movesToTheDayAfterTheLastOneLogged() {
        Routine routine = routine("Pecho", "Espalda", "Pierna");

        assertEquals(1, WorkoutPlanner.nextDayIndex(routine, List.of(session(1, "Pecho", 2))));
        assertEquals(2, WorkoutPlanner.nextDayIndex(routine,
                List.of(session(1, "Pecho", 2), session(2, "Espalda", 4))));
    }

    @Test
    void comesBackToTheFirstDayAfterTheLastDayOfTheRoutine() {
        Routine routine = routine("Pecho", "Espalda", "Pierna");

        assertEquals(0, WorkoutPlanner.nextDayIndex(routine, List.of(session(1, "Pierna", 6))));
    }

    @Test
    void theNewestSessionDecidesWhateverTheOrderOfTheList() {
        Routine routine = routine("Pecho", "Espalda", "Pierna");
        List<TrainingSession> sessions = List.of(session(2, "Espalda", 4), session(1, "Pecho", 2));

        assertEquals(2, WorkoutPlanner.nextDayIndex(routine, sessions));
    }

    @Test
    void aDayThatIsNotInTheRoutineStartsAgainFromTheFirstDay() {
        assertEquals(0, WorkoutPlanner.nextDayIndex(routine("Pecho", "Espalda"), List.of(session(1, "Cardio", 2))));
    }

    @Test
    void aRoutineWithoutDaysHasNothingToTrain() {
        assertEquals(-1, WorkoutPlanner.nextDayIndex(routine(), List.of()));
    }
}
