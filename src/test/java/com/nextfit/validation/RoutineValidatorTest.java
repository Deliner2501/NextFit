package com.nextfit.validation;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoutineValidatorTest {

    private final RoutineValidator validator = new RoutineValidator();

    private static RoutineExercise line(long exercise, int sets, int reps, int rest) {
        return new RoutineExercise(exercise, sets, reps, rest);
    }

    private static Routine routine(String name, String objective, long trainer, List<RoutineDay> days) {
        return new Routine(0, name, objective, trainer, LocalDate.now(), days);
    }

    private static List<RoutineDay> oneDay() {
        return List.of(new RoutineDay("Espalda y bíceps", List.of(line(1, 4, 10, 60))));
    }

    private String badField(Routine routine) {
        return assertThrows(InvalidDataException.class, () -> validator.validate(routine)).getFieldName();
    }

    @Test
    void aCompleteRoutineIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(routine("Rutina de prueba", "Ganar masa muscular", 1, oneDay())));
    }

    @Test
    void theNameAndTheObjectiveAreRequiredAndLimited() {
        assertEquals("nombre de la rutina", badField(routine(" ", "Objetivo", 1, oneDay())));
        assertThrows(InvalidDataException.class, () -> validator.validateName("ab"));
        assertThrows(InvalidDataException.class, () -> validator.validateName("a".repeat(61)));
        assertEquals("objetivo", badField(routine("Rutina", " ", 1, oneDay())));
        assertDoesNotThrow(() -> validator.validateObjective("a".repeat(100)));
        assertThrows(InvalidDataException.class, () -> validator.validateObjective("a".repeat(101)));
    }

    @Test
    void aTrainerMustBeChosen() {
        assertEquals("entrenador", badField(routine("Rutina", "Objetivo", 0, oneDay())));
    }

    @Test
    void aRoutineNeedsAtLeastOneDayWithAtLeastOneExercise() {
        assertEquals("días", badField(routine("Rutina", "Objetivo", 1, List.of())));
        assertEquals("ejercicios",
                badField(routine("Rutina", "Objetivo", 1, List.of(new RoutineDay("Lunes", List.of())))));
        assertEquals("nombre del día", badField(routine("Rutina", "Objetivo", 1,
                List.of(new RoutineDay(" ", List.of(line(1, 3, 10, 60)))))));
    }

    @Test
    void everyDayIsCheckedNotOnlyTheFirst() {
        List<RoutineDay> days = List.of(
                new RoutineDay("Lunes", List.of(line(1, 3, 10, 60))),
                new RoutineDay("Martes", List.of(line(1, 99, 10, 60))));
        assertEquals("series", badField(routine("Rutina", "Objetivo", 1, days)));
    }

    @Test
    void theLinesHaveTheirOwnRanges() {
        assertEquals("ejercicio", assertThrows(InvalidDataException.class,
                () -> validator.validateRoutineExercise(line(0, 3, 10, 60))).getFieldName());
        assertThrows(InvalidDataException.class, () -> validator.validateRoutineExercise(line(1, 0, 10, 60)));
        assertDoesNotThrow(() -> validator.validateRoutineExercise(line(1, 10, 100, 600)));
        assertThrows(InvalidDataException.class, () -> validator.validateRoutineExercise(line(1, 11, 10, 60)));
        assertThrows(InvalidDataException.class, () -> validator.validateRoutineExercise(line(1, 3, 101, 60)));
        assertDoesNotThrow(() -> validator.validateRoutineExercise(line(1, 3, 10, 0)));
        assertThrows(InvalidDataException.class, () -> validator.validateRoutineExercise(line(1, 3, 10, 601)));
        assertThrows(InvalidDataException.class, () -> validator.validateRoutineExercise(line(1, 3, 10, -1)));
    }
}
