package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.service.portal.PortalExercise;
import com.nextfit.service.portal.PortalWorkout;

import java.util.Map;
import java.util.Optional;

/**
 * Tool "my next workout" for the member: the day of his routine that he has to train next, with
 * its exercises. It takes no arguments: it always answers about the member who asks.
 */
public class MyNextWorkoutTool implements AiTool {

    private final MemberSummaryLookup lookup;

    /**
     * Creates the tool.
     *
     * @param lookup finds what the member sees in the portal
     */
    public MyNextWorkoutTool(MemberSummaryLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public String name() {
        return "my_next_workout";
    }

    @Override
    public String description() {
        return "Dice qué día de su rutina le toca entrenar al socio que pregunta y qué ejercicios incluye, "
                + "con series, repeticiones y descanso. Úsala para preguntas como \"¿qué me toca hoy?\".";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return AiTool.noInput();
    }

    @Override
    public boolean allowedFor(AiAudience audience) {
        return audience == AiAudience.MEMBER;
    }

    @Override
    public String run(Map<String, Object> input, AiUser user) {
        Optional<PortalWorkout> workout = lookup.forUser(user).workout();
        if (workout.isEmpty()) {
            return "No tiene una rutina asignada. Debe pedírsela a su entrenador.";
        }
        PortalWorkout day = workout.get();
        StringBuilder text = new StringBuilder("Le toca: ").append(day.dayName()).append(" (rutina ")
                .append(day.routineName()).append(", día ").append(day.dayNumber()).append(" de ")
                .append(day.dayCount()).append(").");
        if (day.exercises().isEmpty()) {
            return text.append("\nEste día no tiene ejercicios todavía.").toString();
        }
        text.append("\nEjercicios:");
        for (PortalExercise exercise : day.exercises()) {
            text.append("\n- ").append(exercise.name()).append(" (").append(exercise.muscleGroup()).append("): ")
                    .append(exercise.sets()).append(" series de ").append(exercise.repetitions())
                    .append(" repeticiones, descanso ").append(exercise.restSeconds()).append(" s");
        }
        return text.toString();
    }
}