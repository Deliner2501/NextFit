package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.model.PaymentMethod;
import com.nextfit.service.PaymentService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tool "revenue summary": tells the AI how much money the gym collected in a period and with
 * which payment method. It only reads. Money is for the administrator alone, so the reception,
 * the trainer and the members never get it, even if the AI asks for the tool.
 */
public class RevenueSummaryTool implements AiTool {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d/MM/yyyy");
    private static final Locale CO = Locale.forLanguageTag("es-CO");

    private final PaymentService payments;
    private final Clock clock;

    /**
     * Creates the tool.
     *
     * @param payments the payments of the gym
     * @param clock    the source of today's date
     */
    public RevenueSummaryTool(PaymentService payments, Clock clock) {
        this.payments = payments;
        this.clock = clock;
    }

    @Override
    public String name() {
        return "revenue_summary";
    }

    @Override
    public String description() {
        return "Dice cuánto dinero recaudó el gimnasio en un periodo y con qué método de pago "
                + "(efectivo, tarjeta, transferencia). Úsala para preguntas como \"¿cuánto vendimos este mes?\".";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> period = new LinkedHashMap<>();
        period.put("type", "string");
        period.put("enum", List.of("hoy", "este_mes", "mes_anterior"));
        period.put("description", "Periodo a consultar. Por defecto \"este_mes\".");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", period);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public boolean allowedFor(AiAudience audience) {
        return audience == AiAudience.ADMIN;
    }

    @Override
    public String run(Map<String, Object> input, AiUser user) {
        LocalDate today = LocalDate.now(clock);
        String period = input.get("period") == null ? "este_mes" : String.valueOf(input.get("period"));
        LocalDate from;
        LocalDate to;
        String label;
        switch (period) {
            case "hoy" -> {
                from = today;
                to = today;
                label = "hoy";
            }
            case "este_mes" -> {
                from = today.withDayOfMonth(1);
                to = today;
                label = "este mes";
            }
            case "mes_anterior" -> {
                LocalDate previous = today.minusMonths(1);
                from = previous.withDayOfMonth(1);
                to = previous.withDayOfMonth(previous.lengthOfMonth());
                label = "el mes anterior";
            }
            default -> throw new InvalidDataException("periodo", "debe ser \"hoy\", \"este_mes\" o \"mes_anterior\"");
        }
        StringBuilder text = new StringBuilder();
        text.append("Ingresos de ").append(label).append(" (del ").append(from.format(DAY))
                .append(" al ").append(to.format(DAY)).append("): ")
                .append(money(payments.revenueBetween(from, to))).append(".\nPor método de pago:");
        for (Map.Entry<PaymentMethod, Long> entry : payments.revenueByMethod(from, to).entrySet()) {
            text.append(" ").append(entry.getKey().getLabel()).append(" ").append(money(entry.getValue())).append(";");
        }
        return text.toString();
    }

    // EXPLICACIÓN: pesos colombianos con punto de miles, como se lee en el resto de la aplicación
    private static String money(long pesos) {
        return "$" + String.format(CO, "%,d", pesos);
    }
}