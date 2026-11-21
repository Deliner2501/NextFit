package com.nextfit.service.alert;

import com.nextfit.model.Member;
import com.nextfit.service.MemberService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the active members whose birthday is today or in the next few days.
 * Someone born on 29 February celebrates on 28 February in the years that are
 * not leap years.
 */
public class BirthdayRule implements AlertRule {

    private final MemberService members;
    private final int daysAhead;

    /**
     * Creates the rule.
     *
     * @param members   the service that knows the members
     * @param daysAhead how many days after today are still reported; 0 reports only today
     */
    public BirthdayRule(MemberService members, int daysAhead) {
        this.members = members;
        this.daysAhead = daysAhead;
    }

    @Override
    public List<Alert> evaluate(LocalDate today) {
        List<Alert> result = new ArrayList<>();
        for (Member member : members.findAll()) {
            if (!member.isActive()) {
                continue;
            }
            LocalDate birthday = nextBirthday(member.getBirthDate(), today);
            long offset = ChronoUnit.DAYS.between(today, birthday);
            if (offset <= daysAhead) {
                result.add(new Alert(AlertType.BIRTHDAY, member, member.ageOn(birthday), birthday));
            }
        }
        result.sort(Comparator.comparing(Alert::date).thenComparing(a -> a.member().getFullName()));
        return result;
    }

    private static LocalDate nextBirthday(LocalDate birthDate, LocalDate today) {
        LocalDate thisYear = birthDate.withYear(today.getYear());
        return thisYear.isBefore(today) ? birthDate.withYear(today.getYear() + 1) : thisYear;
    }
}
