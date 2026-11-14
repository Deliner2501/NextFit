package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiTool;
import com.nextfit.ai.AiUser;
import com.nextfit.service.portal.PortalMembership;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Tool "my membership" for the member: his plan, until when it is valid and how many days are left.
 * It takes no arguments on purpose: the member is the one who asks, so there is nobody else to name.
 */
public class MyMembershipTool implements AiTool {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-CO"));

    private final MemberSummaryLookup lookup;

    /**
     * Creates the tool.
     *
     * @param lookup finds what the member sees in the portal
     */
    public MyMembershipTool(MemberSummaryLookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public String name() {
        return "my_membership";
    }

    @Override
    public String description() {
        return "Dice cuál es la membresía del socio que pregunta: el plan, hasta cuándo es válida y cuántos días "
                + "le quedan. Úsala para preguntas como \"¿cuándo vence mi membresía?\".";
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
        Optional<PortalMembership> membership = lookup.forUser(user).membership();
        if (membership.isEmpty()) {
            return "No tiene una membresía vigente. Debe pasar por recepción para renovarla.";
        }
        PortalMembership current = membership.get();
        String left = current.daysLeft() == 0 ? "Vence hoy." : "Le quedan " + current.daysLeft() + " día(s).";
        return "Plan: " + current.planName() + ". Vigente hasta el " + current.validUntil().format(DAY) + ". " + left;
    }
}