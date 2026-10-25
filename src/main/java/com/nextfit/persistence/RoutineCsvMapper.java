package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;
import com.nextfit.model.Routine;
import com.nextfit.model.RoutineDay;
import com.nextfit.model.RoutineExercise;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts routines to and from CSV rows. A routine is stored in two files:
 * one row per routine, and one row per exercise line of its days.
 */
public final class RoutineCsvMapper {

    /** Header of the routines file. */
    public static final String[] ROUTINE_COLUMNS = {"id", "name", "objective", "trainerId", "createdAt"};

    /** Header of the file with the exercise lines of every routine. */
    public static final String[] LINE_COLUMNS = {"routineId", "dayIndex", "dayName",
            "exerciseId", "sets", "repetitions", "restSeconds"};

    private RoutineCsvMapper() {
    }

    /**
     * Converts the data of a routine (without its days) to a CSV row.
     *
     * @param routine the routine
     * @return the row for the routines file
     */
    public static String[] toRoutineRow(Routine routine) {
        return new String[]{
                String.valueOf(routine.getId()),
                routine.getName(),
                routine.getObjective(),
                String.valueOf(routine.getTrainerId()),
                routine.getCreatedAt().toString()
        };
    }

    /**
     * Converts the days of a routine to CSV rows, one per exercise line.
     *
     * @param routine the routine, already with its identifier
     * @return the rows for the lines file
     */
    public static List<String[]> toLineRows(Routine routine) {
        // EXPLICACIÓN: una rutina tiene días y cada día tiene ejercicios, pero
        // un CSV solo guarda filas planas. Por eso cada ejercicio de cada día
        // es una fila, y dayIndex (0, 1, 2...) dice a qué día pertenece. El
        // orden de las filas conserva el orden de los ejercicios.
        List<String[]> rows = new ArrayList<>();
        List<RoutineDay> days = routine.getDays();
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            RoutineDay day = days.get(dayIndex);
            for (RoutineExercise line : day.getExercises()) {
                rows.add(new String[]{
                        String.valueOf(routine.getId()),
                        String.valueOf(dayIndex),
                        day.getName(),
                        String.valueOf(line.getExerciseId()),
                        String.valueOf(line.getSets()),
                        String.valueOf(line.getRepetitions()),
                        String.valueOf(line.getRestSeconds())
                });
            }
        }
        return rows;
    }

    /**
     * Rebuilds a routine from its row and its exercise lines.
     *
     * @param routineRow the row of the routines file
     * @param lineRows   the rows of the lines file that belong to this routine
     * @return the routine
     * @throws PersistenceException when a row is malformed
     */
    public static Routine fromRows(String[] routineRow, List<String[]> lineRows) {
        try {
            // EXPLICACIÓN: se agrupan las filas por dayIndex para reconstruir
            // cada día. TreeMap las deja ordenadas por número de día.
            Map<Integer, String> dayNames = new TreeMap<>();
            Map<Integer, List<RoutineExercise>> dayExercises = new TreeMap<>();
            for (String[] row : lineRows) {
                int dayIndex = Integer.parseInt(row[1]);
                dayNames.putIfAbsent(dayIndex, row[2]);
                dayExercises.computeIfAbsent(dayIndex, k -> new ArrayList<>()).add(
                        new RoutineExercise(
                                Long.parseLong(row[3]),
                                Integer.parseInt(row[4]),
                                Integer.parseInt(row[5]),
                                Integer.parseInt(row[6])));
            }
            List<RoutineDay> days = new ArrayList<>();
            for (Map.Entry<Integer, List<RoutineExercise>> entry : dayExercises.entrySet()) {
                days.add(new RoutineDay(dayNames.get(entry.getKey()), entry.getValue()));
            }
            return new Routine(
                    Long.parseLong(routineRow[0]),
                    routineRow[1],
                    routineRow[2],
                    Long.parseLong(routineRow[3]),
                    LocalDate.parse(routineRow[4]),
                    days);
        } catch (RuntimeException e) {
            throw new PersistenceException("Fila de rutina inválida en los archivos de rutinas", e);
        }
    }
}