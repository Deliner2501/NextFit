package com.nextfit.service.suggest;

import com.nextfit.model.Difficulty;
import com.nextfit.model.MuscleGroup;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Adjusts the routine according to the medical conditions written in the health profile.
 * It looks for key words in the text: knee problems limit leg exercises, back problems
 * limit back exercises, and heart or blood pressure problems lengthen the rest and
 * remove advanced exercises.
 */
public class MedicalConditionRule implements SuggestionRule {

    private static final int HEART_REST_SECONDS = 90;

    @Override
    public SuggestionSettings apply(SuggestionSettings settings, SuggestionRequest request) {
        // EXPLICACIÓN: el texto médico lo escribe una persona, así que se pasa a
        // minúsculas y sin tildes para que "Rodilla", "rodilla" o "HIPERTENSIÓN"
        // se reconozcan igual.
        String text = normalize(request.medicalConditions());
        SuggestionSettings result = settings;
        if (mentions(text, "rodilla")) {
            result = result.withGroupCap(MuscleGroup.LEGS, Difficulty.INTERMEDIATE)
                    .withNote("Rodilla: se quitaron los ejercicios de piernas de nivel avanzado.");
        }
        if (mentions(text, "espalda", "columna", "hernia")) {
            result = result.withGroupCap(MuscleGroup.BACK, Difficulty.INTERMEDIATE)
                    .withNote("Espalda: se quitaron los ejercicios de espalda de nivel avanzado.");
        }
        if (mentions(text, "hipertension", "corazon", "cardiaca", "cardiaco")) {
            result = result.withRestAtLeast(HEART_REST_SECONDS)
                    .withMaxLevelAtMost(Difficulty.INTERMEDIATE)
                    .withNote("Corazón o presión: descanso de 90 segundos y sin ejercicios avanzados.");
        }
        return result;
    }

    private static boolean mentions(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        // EXPLICACIÓN: NFD separa cada letra de su tilde ("ó" queda "o" + tilde) y
        // el replaceAll borra las tildes sueltas.
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}