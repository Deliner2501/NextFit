package com.nextfit.service;

import com.nextfit.model.Member;

import java.time.LocalDate;

/**
 * What the kiosk shows after a carnet is scanned.
 *
 * @param outcome   what happened
 * @param member    the member, or null when the code matched nobody
 * @param message   a short text for the screen
 * @param validUntil last day of the membership, or null when unknown
 * @param daysLeft  days until the membership ends (0 = ends today), or -1 when unknown
 */
public record AccessResult(Outcome outcome, Member member, String message, LocalDate validUntil, long daysLeft) {

    /** The possible results of a scan. */
    public enum Outcome { ENTRY, EXIT, DENIED }

    /**
     * Tells whether the member was let in or out.
     *
     * @return true for an entry or an exit
     */
    public boolean granted() {
        return outcome != Outcome.DENIED;
    }
}
