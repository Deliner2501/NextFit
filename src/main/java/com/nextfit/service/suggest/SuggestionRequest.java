package com.nextfit.service.suggest;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.BmiCategory;
import com.nextfit.model.Difficulty;
import com.nextfit.model.TrainingGoal;

/**
 * Everything the trainer chooses (and what is known about the member) to ask for a
 * suggested routine.
 *
 * @param goal              what the member wants to reach
 * @param level             experience of the member
 * @param daysPerWeek       training days per week, from 2 to 5
 * @param bmiCategory       body mass index category of the member, or null when unknown
 * @param medicalConditions free text from the health profile, never null
 */
public record SuggestionRequest(TrainingGoal goal, Difficulty level, int daysPerWeek,
                                BmiCategory bmiCategory, String medicalConditions) {

    private static final int MIN_DAYS = 2;
    private static final int MAX_DAYS = 5;

    // EXPLICACIÓN: un record es una clase corta e inmutable: solo guarda datos.
    // Este "constructor compacto" se ejecuta al crear la solicitud y revisa que
    // los datos tengan sentido. Si algo está mal lanza BusinessRuleException,
    // igual que los demás servicios del proyecto.
    public SuggestionRequest {
        if (goal == null) {
            throw new BusinessRuleException("Debe elegir un objetivo");
        }
        if (level == null) {
            throw new BusinessRuleException("Debe elegir un nivel");
        }
        if (daysPerWeek < MIN_DAYS || daysPerWeek > MAX_DAYS) {
            throw new BusinessRuleException(
                    "Los días por semana deben estar entre " + MIN_DAYS + " y " + MAX_DAYS);
        }
        // EXPLICACIÓN: el texto médico nunca queda en null para que las reglas
        // no tengan que revisar eso cada vez.
        medicalConditions = medicalConditions == null ? "" : medicalConditions;
    }
}