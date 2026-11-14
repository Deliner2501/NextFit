package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.service.churn.ChurnRisk;
import com.nextfit.service.churn.ChurnService;
import com.nextfit.service.churn.RiskLevel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool "members at risk": tells the AI which members may leave the gym and why, using the same
 * prediction as the churn screen. It only reads. Retention is a commercial matter, so it is for
 * the administrator and the reception, not for the trainer or the members. It never gives phones
 * or documents, only what is needed to answer.
 */
public class MembersAtRiskTool implements AiTool {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final ChurnService churn;

    /**
     * Creates the tool.
     *
     * @param churn the churn prediction
     */
    public MembersAtRiskTool(ChurnService churn) {
        this.churn = churn;
    }

    @Override
    public String name() {
        return "members_at_risk";
    }

    @Override
    public String description() {
        return "Lista los socios con membresía vigente que podrían abandonar el gimnasio, del más al menos "
                + "riesgoso, con el puntaje de 0 a 100, los motivos y la acción sugerida. "
                + "Úsala para preguntas como \"¿quién se está yendo?\" o \"¿a quién debo llamar hoy?\".";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> level = new LinkedHashMap<>();
        level.put("type", "string");
        level.put("enum", List.of("medio", "alto"));
        level.put("description", "Riesgo mínimo. \"medio\" incluye medio y alto (por defecto); \"alto\" solo el alto.");
        Map<String, Object> limit = new LinkedHashMap<>();
        limit.put("type", "integer");
        limit.put("description", "Máximo de socios a listar, entre 1 y " + MAX_LIMIT + ". Por defecto " + DEFAULT_LIMIT + ".");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("level", level);
        properties.put("limit", limit);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public boolean allowedFor(AiAudience audience) {
        return audience == AiAudience.ADMIN || audience == AiAudience.RECEPTION;
    }

    @Override
    public String run(Map<String, Object> input, AiUser user) {
        RiskLevel minimum = minimumOf(input.get("level"));int limit = ToolInput.limit(input.get("limit"), DEFAULT_LIMIT, MAX_LIMIT);
        List<ChurnRisk> risks = churn.findAtLeast(minimum);
        if (risks.isEmpty()) {
            return "Ningún socio tiene riesgo " + minimum.getLabel().toLowerCase() + " o mayor en este momento.";
        }
        StringBuilder text = new StringBuilder();
        text.append(risks.size()).append(" socio(s) con riesgo ")
                .append(minimum.getLabel().toLowerCase()).append(" o mayor");
        if (risks.size() > limit) {
            text.append(" (se muestran los ").append(limit).append(" primeros)");
        }
        text.append(":\n");
        risks.stream().limit(limit).forEach(risk -> text
                .append("- ").append(risk.member().getFullName())
                .append(": riesgo ").append(risk.level().getLabel().toLowerCase())
                .append(" (").append(risk.score()).append("/100). Motivos: ")
                .append(String.join("; ", risk.reasons()))
                .append(". Acción sugerida: ").append(risk.action()).append(".\n"));
        return text.toString().trim();
    }

    private static RiskLevel minimumOf(Object value) {
        if (value == null) {
            return RiskLevel.MEDIUM;
        }
        return switch (String.valueOf(value).toLowerCase()) {
            case "medio" -> RiskLevel.MEDIUM;
            case "alto" -> RiskLevel.HIGH;
            default -> throw new InvalidDataException("nivel", "debe ser \"medio\" o \"alto\"");
        };
    }
}