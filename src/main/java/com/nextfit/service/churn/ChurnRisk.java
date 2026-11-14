package com.nextfit.service.churn;

import com.nextfit.model.Member;

import java.util.List;

/**
 * The risk that one member stops coming. It is a plain value: it does not know how
 * it was calculated or how it will be shown.
 *
 * @param member  the member
 * @param score   the risk from 0 (none) to 100 (very high)
 * @param level   the level that the score reaches
 * @param reasons why the member has this risk, the heaviest reason first; empty when there is none
 */
public record ChurnRisk(Member member, int score, RiskLevel level, List<String> reasons) {

    /** Keeps its own copy of the reasons, so nobody can change them later. */
    public ChurnRisk {
        reasons = List.copyOf(reasons);
    }

    /**
     * Returns what the staff should do with this member.
     *
     * @return the suggested action, in Spanish
     */
    public String action() {
        return level.getAction();
    }
}