package com.nextfit.service.alert;

import com.nextfit.model.Member;

import java.time.LocalDate;

/**
 * One thing the staff should know about a member. It is a plain value: it has
 * no behavior and does not know how it will be shown or sent.
 *
 * <p>The meaning of {@code days} and {@code date} depends on the type:
 * <ul>
 *   <li>{@link AlertType#EXPIRING}: days left and the last valid day;</li>
 *   <li>{@link AlertType#INACTIVE}: days without visiting and the day of the last visit
 *       (or the day the membership started when there are no visits);</li>
 *   <li>{@link AlertType#BIRTHDAY}: the age the member turns and the day of the birthday.</li>
 * </ul>
 *
 * @param type   the kind of alert
 * @param member the member the alert is about
 * @param days   the number that goes with the alert, see above
 * @param date   the date that goes with the alert, see above
 */
public record Alert(AlertType type, Member member, long days, LocalDate date) {
}
