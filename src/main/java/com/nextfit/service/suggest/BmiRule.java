package com.nextfit.service.suggest;

import com.nextfit.model.BmiCategory;
import com.nextfit.model.Difficulty;

/**
 * Adjusts the routine according to the body mass index category of the member: more
 * cardio and a lower difficulty limit for overweight and obesity, and no extra cardio
 * for underweight.
 */
public class BmiRule implements SuggestionRule {

    @Override
    public SuggestionSettings apply(SuggestionSettings settings, SuggestionRequest request) {
        BmiCategory category = request.bmiCategory();
        // EXPLICACIÓN: si no se conoce el IMC del socio (todavía no tiene peso o
        // estatura registrados), la regla no cambia nada.
        if (category == null) {
            return settings;
        }
        return switch (category) {
            case OVERWEIGHT -> settings.withCardioEveryDay()
                    .withNote("Sobrepeso: se agregó un ejercicio de cardio en cada día.");
            case OBESITY -> settings.withCardioEveryDay()
                    .withMaxLevelAtMost(Difficulty.INTERMEDIATE)
                    .withNote("Obesidad: se agregó cardio en cada día y se limitó el nivel a intermedio.");
            case UNDERWEIGHT -> settings.withoutCardioEveryDay()
                    .withNote("Bajo peso: no se agregó cardio extra.");
            case NORMAL -> settings;
        };
    }
}