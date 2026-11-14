package com.nextfit.service.churn;

import com.nextfit.model.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChurnRiskTest {

    private static Member member() {
        return new Member(1, "Luis Pérez", "20000", "3001234567", "socio@example.com",
                LocalDate.of(1998, 3, 1), LocalDate.of(2026, 1, 5), "", "Mamá 3110000000", true);
    }

    @Test
    void theDetailShowsEveryReasonCompleteAndTheAction() {
        ChurnRisk risk = new ChurnRisk(member(), 41, RiskLevel.MEDIUM,
                List.of("Pasó de 3 a 1 visitas por semana", "Su membresía vence en 2 días y no ha renovado"));

        String detail = risk.detail();

        assertTrue(detail.contains("Riesgo medio (41/100)"));
        assertTrue(detail.contains("- Pasó de 3 a 1 visitas por semana"));
        assertTrue(detail.contains("- Su membresía vence en 2 días y no ha renovado"));
        assertTrue(detail.contains("Qué hacer: " + RiskLevel.MEDIUM.getAction()));
    }

    @Test
    void withoutReasonsTheDetailSaysThereAreNoSigns() {
        ChurnRisk risk = new ChurnRisk(member(), 0, RiskLevel.LOW, List.of());

        assertTrue(risk.detail().contains("- Sin señales de abandono"));
    }
}