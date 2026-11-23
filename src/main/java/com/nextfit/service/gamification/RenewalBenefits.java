package com.nextfit.service.gamification;

import com.nextfit.model.Member;
import com.nextfit.model.Payment;
import com.nextfit.model.RenewalDiscount;
import com.nextfit.service.MemberService;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Determines which special benefits a returning member has yet to redeem. */
public final class RenewalBenefits {
    private final MemberService members;
    private final GamificationService achievements;

    /**
     * @param members the source of birth dates
     * @param achievements the source of achievements
     */
    public RenewalBenefits(MemberService members, GamificationService achievements) {
        this.members = members;
        this.achievements = achievements;
    }

    /**
     * @param memberId the member renewing
     * @param history the member's previously confirmed payments
     * @param today the date on which the payment is made
     * @return eligible benefits, highest percentage first
     */
    public List<RenewalDiscount> available(long memberId, List<Payment> history, LocalDate today) {
        if (history.isEmpty()) {
            return List.of();
        }
        Member member = members.findById(memberId);
        if (!member.isActive()) {
            return List.of();
        }
        List<RenewalDiscount> available = new ArrayList<>();
        LocalDate birth = member.getBirthDate();
        if (birth != null && MonthDay.from(birth).atYear(today.getYear()).equals(today)) {
            var birthday = new RenewalDiscount(RenewalDiscount.BIRTHDAY, 50);
            if (history.stream().noneMatch(payment -> payment.getPaidAt().getYear() == today.getYear()
                    && payment.getDiscount().getType().equals(birthday.getType()))) {
                available.add(birthday);
            }
        }
        for (Badge badge : achievements.achievementsOf(memberId).badges()) {
            var reward = new RenewalDiscount(badge.name(), percentage(badge));
            if (history.stream().noneMatch(payment -> payment.getDiscount().getType().equals(reward.getType()))) {
                available.add(reward);
            }
        }
        return available.stream().sorted(Comparator.comparingLong(RenewalDiscount::getValue).reversed()).toList();
    }

    private static int percentage(Badge badge) {
        return switch (badge) {
            case FIRST_STEP -> 10;
            case STEADY -> 15;
            case MONTH_STREAK -> 20;
            case UNSTOPPABLE -> 25;
            case QUARTER_STREAK -> 30;
            case LEGEND -> 40;
        };
    }
}
