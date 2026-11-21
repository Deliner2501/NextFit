package com.nextfit.service.churn;

/**
 * What one rule found about a member: the points it adds to the risk and the
 * reason in plain words. It is a plain value with no behavior.
 *
 * @param points the points added, from 0 up to the maximum of the rule
 * @param reason why, in Spanish and for the staff to read
 */
public record ChurnFactor(int points, String reason) {
}
