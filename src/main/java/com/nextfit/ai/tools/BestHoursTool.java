package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.service.occupancy.HourForecast;
import com.nextfit.service.portal.PortalBestTimes;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tool "best hours" for the member: the quietest hours to train, from the forecast of the
 * occupation of the gym. It takes no arguments.
 */
public class BestHoursTool implements AiTool {

    private final MemberSummaryLookup lookup;

    /**
     * Creates the tool.
     *
     * @param lookup finds what the member sees in the portal
     */
    public BestHoursTool(MemberSummaryLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public String name() {
        return "best_hours_to_train";
    }

    @Override
    public String description() {
        return "Dice cuáles son las horas más tranquilas para entrenar hoy o mañana, según cuánta gente suele haber "
                + "en el gimnasio. Úsala para preguntas como \"¿a qué hora es mejor ir?\".";
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
        Optional<PortalBestTimes> times = lookup.forUser(user).bestTimes();
        if (times.isEmpty()) {
            return "Todavía no hay suficientes visitas para recomendar una hora.";
        }
        PortalBestTimes best = times.get();
        String hours = best.hours().stream()
                .map(this::describe)
                .collect(Collectors.joining(", "));
        return best.dayLabel() + " el gimnasio estará más tranquilo a las: " + hours + ".";
    }

    private String describe(HourForecast hour) {
        return String.format("%02d:00 (%s)", hour.hour(), hour.level().getLabel());
    }
}