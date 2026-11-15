package com.nextfit.service.portal;

import java.time.Duration;
import java.util.Optional;

/**
 * What a member sees right after the QR scan: "your day" when entering and "your summary" when
 * leaving. It is a plain value; it does not know how it will be drawn.
 *
 * @param kind         whether it is the summary of an entry or of an exit
 * @param workout      the day of the routine to train (entry) or the one that comes next (exit)
 * @param membership   the valid membership, or empty when there is none
 * @param stay         how long the member stayed inside; empty when entering or when it is unknown
 * @param trainingDays the different days the member has come to the gym
 * @param streak       the weeks in a row the member has come
 * @param bestTimes    the quietest hours to train; only filled when entering
 */
public record VisitSummary(Kind kind, Optional<PortalWorkout> workout, Optional<PortalMembership> membership,
                           Optional<Duration> stay, int trainingDays, int streak,
                           Optional<PortalBestTimes> bestTimes) {

    /** The moment of the visit the summary is for. */
    public enum Kind { ENTRY, EXIT }
}
