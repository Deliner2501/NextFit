package com.nextfit.ai.tools;

import com.nextfit.ai.AiAudience;
import com.nextfit.ai.AiUser;
import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.service.portal.PortalSummary;

import java.util.Optional;

/**
 * Finds what a member sees in the phone portal. The tools of the member use it, so they do not
 * know where the data comes from. Who is asking always comes from {@link AiUser}, which the server
 * fills from the access code of the carnet; it never comes from what the member writes, so one
 * member cannot read the data of another.
 */
@FunctionalInterface
public interface MemberSummaryLookup {

    /**
     * Finds the summary of a member.
     *
     * @param memberId the member
     * @return the summary, or empty when the member does not exist
     */
    Optional<PortalSummary> find(long memberId);

    /**
     * Finds the summary of the member who is asking and refuses anyone else.
     *
     * @param user who asks
     * @return the summary of that same member
     * @throws BusinessRuleException when the person is not a member, or the member does not exist
     */
    default PortalSummary forUser(AiUser user) {
        if (user.audience() != AiAudience.MEMBER || user.memberId() <= 0) {
            throw new BusinessRuleException("Solo un socio puede consultar sus propios datos");
        }
        return find(user.memberId()).orElseThrow(() -> new BusinessRuleException("No se encontró al socio"));
    }
}