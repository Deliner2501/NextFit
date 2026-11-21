package com.nextfit.service.suggest;

import static com.nextfit.model.MuscleGroup.ARMS;
import static com.nextfit.model.MuscleGroup.BACK;
import static com.nextfit.model.MuscleGroup.CHEST;
import static com.nextfit.model.MuscleGroup.CORE;
import static com.nextfit.model.MuscleGroup.GLUTES;
import static com.nextfit.model.MuscleGroup.LEGS;
import static com.nextfit.model.MuscleGroup.SHOULDERS;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.MuscleGroup;
import java.util.List;

/**
 * Decides which muscle groups are trained on each day of the week. Every day is a list
 * of slots, and each slot is one exercise of that muscle group.
 */
public final class WeeklySplit {

    /**
     * One training day: its name and the muscle group of each exercise slot.
     *
     * @param name  the name shown for the day
     * @param slots the muscle group of each exercise, in order
     */
    public record DayTemplate(String name, List<MuscleGroup> slots) {
    }

    private WeeklySplit() {
        // EXPLICACIÓN: constructor privado porque esta clase solo tiene métodos
        // estáticos; nadie necesita crear objetos WeeklySplit.
    }

    /**
     * Returns the day templates for a number of training days per week.
     *
     * @param days training days per week, from 2 to 5
     * @return one template per day
     * @throws BusinessRuleException when the number of days is not supported
     */
    public static List<DayTemplate> forDays(int days) {
        return switch (days) {
            case 2 -> List.of(
                    new DayTemplate("Día 1 · Cuerpo completo", List.of(CHEST, BACK, LEGS, SHOULDERS, CORE, ARMS)),
                    new DayTemplate("Día 2 · Cuerpo completo", List.of(LEGS, CHEST, BACK, SHOULDERS, CORE, ARMS)));
            case 3 -> List.of(
                    new DayTemplate("Día 1 · Pecho y hombros", List.of(CHEST, CHEST, SHOULDERS, SHOULDERS, CORE, ARMS)),
                    new DayTemplate("Día 2 · Espalda y brazos", List.of(BACK, BACK, ARMS, ARMS, CORE, SHOULDERS)),
                    new DayTemplate("Día 3 · Piernas y glúteos", List.of(LEGS, LEGS, GLUTES, GLUTES, LEGS, CORE)));
            case 4 -> List.of(
                    new DayTemplate("Día 1 · Tren superior", List.of(CHEST, BACK, SHOULDERS, ARMS, CHEST, BACK)),
                    new DayTemplate("Día 2 · Tren inferior", List.of(LEGS, GLUTES, LEGS, GLUTES, CORE, LEGS)),
                    new DayTemplate("Día 3 · Tren superior", List.of(BACK, CHEST, ARMS, SHOULDERS, BACK, CHEST)),
                    new DayTemplate("Día 4 · Tren inferior", List.of(GLUTES, LEGS, GLUTES, LEGS, CORE, LEGS)));
            case 5 -> List.of(
                    new DayTemplate("Día 1 · Pecho", List.of(CHEST, CHEST, CHEST, CHEST, ARMS, CORE)),
                    new DayTemplate("Día 2 · Espalda", List.of(BACK, BACK, BACK, BACK, ARMS, CORE)),
                    new DayTemplate("Día 3 · Piernas", List.of(LEGS, LEGS, LEGS, GLUTES, GLUTES, CORE)),
                    new DayTemplate("Día 4 · Hombros", List.of(SHOULDERS, SHOULDERS, SHOULDERS, SHOULDERS, ARMS, CORE)),
                    new DayTemplate("Día 5 · Brazos y abdomen", List.of(ARMS, ARMS, ARMS, ARMS, CORE, CORE)));
            default -> throw new BusinessRuleException("Solo hay reparto para 2 a 5 días por semana");
        };
    }
}