package com.nextfit.service.portal;

import java.time.LocalDate;

/**
 * The membership of a member as the portal shows it.
 *
 * @param planName   the name of the plan
 * @param validUntil the last valid day
 * @param daysLeft   the days left, counting from today
 */
public record PortalMembership(String planName, LocalDate validUntil, long daysLeft) {
}
