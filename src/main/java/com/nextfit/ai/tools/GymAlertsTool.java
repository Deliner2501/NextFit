package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.service.alert.Alert;
import com.nextfit.service.alert.AlertService;
import com.nextfit.service.alert.AlertType;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool "gym alerts": tells the AI which memberships are about to expire, which members stopped
 * coming and who has a birthday today, the same alerts as the alerts screen. It only reads, and it
 * is for the administrator and the reception, who are the ones that contact the members.
 */
public class GymAlertsTool implements AiTool {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d/MM/yyyy");

    private final AlertService alerts;

    /**
     * Creates the tool.
     *
     * @param alerts the alerts of the gym
     */
    public GymAlertsTool(AlertService alerts) {
        this.alerts = alerts;
    }

    @Override
    public String name() {
        return "gym_alerts";
    }

    @Override
    public String description() {
        return "Lista las alertas del gimnasio: membresías por vencer, socios que dejaron de venir y "
                + "cumpleaños de hoy. Úsala para preguntas como \"¿a quién se le vence la membresía?\" "
                + "o \"¿quién cumple años hoy?\".";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> type = new LinkedHashMap<>();
        type.put("type", "string");
        type.put("enum", List.of("por_vencer", "inactivos", "cumpleanos"));
        type.put("description", "Tipo de alerta. Si no se indica, se muestran los tres tipos.");
        Map<String, Object> limit = new LinkedHashMap<>();
        limit.put("type", "integer");
        limit.put("description", "Máximo de socios por tipo, entre 1 y " + MAX_LIMIT + ". Por defecto " + DEFAULT_LIMIT + ".");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("type", type);
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
        List<AlertType> types = typesOf(input.get("type"));
        int limit = ToolInput.limit(input.get("limit"), DEFAULT_LIMIT, MAX_LIMIT);
        StringBuilder text = new StringBuilder();
        for (AlertType type : types) {
            List<Alert> found = alerts.findByType(type);
            if (found.isEmpty()) {
                continue;
            }
            text.append(type.getLabel()).append(" (").append(found.size()).append("):\n");
            found.stream().limit(limit).forEach(alert -> text.append("- ").append(describe(alert)).append("\n"));
            if (found.size() > limit) {
                text.append("(se muestran los ").append(limit).append(" primeros)\n");
            }
        }
        return text.isEmpty() ? "No hay alertas de ese tipo por ahora." : text.toString().trim();
    }

    private static List<AlertType> typesOf(Object value) {
        if (value == null) {
            return List.of(AlertType.values());
        }
        return switch (String.valueOf(value)) {
            case "por_vencer" -> List.of(AlertType.EXPIRING);
            case "inactivos" -> List.of(AlertType.INACTIVE);
            case "cumpleanos" -> List.of(AlertType.BIRTHDAY);
            default -> throw new InvalidDataException("tipo", "debe ser \"por_vencer\", \"inactivos\" o \"cumpleanos\"");
        };
    }

    // EXPLICACIÓN: el campo "days" de la alerta significa cosas distintas según el tipo
    private static String describe(Alert alert) {
        String name = alert.member().getFullName();
        return switch (alert.type()) {
            case EXPIRING -> name + ": su membresía vence "
                    + (alert.days() == 0 ? "hoy" : "en " + alert.days() + " día(s)")
                    + " (" + alert.date().format(DAY) + ")";
            case INACTIVE -> name + ": lleva " + alert.days() + " días sin venir";
            case BIRTHDAY -> name + ": cumple " + alert.days() + " años hoy";
        };
    }
}