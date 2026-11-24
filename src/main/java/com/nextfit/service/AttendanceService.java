package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.AttendanceRecord;
import com.nextfit.model.Member;
import com.nextfit.persistence.AttendanceRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Rules of the entry and exit of members. A member may enter only while
 * registered, with a valid membership and not already inside. The membership
 * check goes through {@link SubscriptionChecker}, so this service does not
 * depend on the membership module and can be tested with a fake one.
 */
public class AttendanceService {

    private final MemberService memberService;
    private final AttendanceRepository attendance;
    private final SubscriptionChecker subscriptionChecker;
    private final Clock clock;
    private final IntSupplier capacity;

    /**
     * Creates the service using the system clock.
     *
     * @param memberService       the service used to find the member
     * @param attendance          the attendance storage
     * @param subscriptionChecker answers whether a member has a valid membership
     */
    public AttendanceService(MemberService memberService, AttendanceRepository attendance,
                             SubscriptionChecker subscriptionChecker) {
        this(memberService, attendance, subscriptionChecker, Clock.systemDefaultZone());
    }

    /**
     * Creates the service with a given clock, so tests can fix the time.
     *
     * @param memberService       the service used to find the member
     * @param attendance          the attendance storage
     * @param subscriptionChecker answers whether a member has a valid membership
     * @param clock               the clock that tells the current time
     */
    public AttendanceService(MemberService memberService, AttendanceRepository attendance,
                             SubscriptionChecker subscriptionChecker, Clock clock) {
        this(memberService, attendance, subscriptionChecker, clock, () -> Integer.MAX_VALUE);
    }

    /**
     * Creates the admission service with a live capacity setting.
     *
     * @param memberService the source of member information
     * @param attendance the attendance repository
     * @param subscriptionChecker the membership rules
     * @param clock the current time
     * @param capacity the current admission limit
     */
    public AttendanceService(MemberService memberService, AttendanceRepository attendance,
                             SubscriptionChecker subscriptionChecker, Clock clock, IntSupplier capacity) {
        this.memberService = memberService;
        this.attendance = attendance;
        this.capacity = capacity;
        this.subscriptionChecker = subscriptionChecker;
        this.clock = clock;
    }

    /**
     * Records the entry of a member.
     *
     * @param memberId the identifier of the member
     * @return the stored record, still open
     * @throws com.nextfit.exceptions.ResourceNotFoundException when the member does not exist
     * @throws BusinessRuleException                            when the member is inactive, has no
     *                                                          valid membership or is already inside
     */
    public synchronized AttendanceRecord checkIn(long memberId) {
        Member member = memberService.findById(memberId);
        if (!member.isActive()) {
            throw new BusinessRuleException("El socio " + member.getFullName() + " está dado de baja");
        }
        if (!subscriptionChecker.hasActiveSubscription(memberId)) {
            throw new BusinessRuleException(
                    "El socio " + member.getFullName() + " no tiene una membresía vigente");
        }
        if (attendance.findOpenByMemberId(memberId).isPresent()) {
            throw new BusinessRuleException("El socio " + member.getFullName() + " ya está dentro del gimnasio");
        }
        int limit = capacity.getAsInt();
        if (limit != Integer.MAX_VALUE && attendance.countOpen() >= limit) {
            throw new BusinessRuleException("Aforo completo (" + limit + " personas). Tu membresía sigue activa. Puedes volver a intentar cuando se libere un cupo.");
        }
        return attendance.save(new AttendanceRecord(0, memberId, LocalDateTime.now(clock), null));
    }

    /** @return the number of visits currently open */
    public long countInside() {
        return attendance.countOpen();
    }

    /**
     * Records the exit of a member.
     *
     * @param memberId the identifier of the member
     * @return the stored record, now closed
     * @throws BusinessRuleException when the member has no entry without an exit
     */
    public synchronized AttendanceRecord checkOut(long memberId) {
        AttendanceRecord open = attendance.findOpenByMemberId(memberId)
                .orElseThrow(() -> new BusinessRuleException("El socio no tiene una entrada abierta"));
        return attendance.save(open.withCheckOut(LocalDateTime.now(clock)));
    }

    /**
     * Returns the members who are inside the gym right now.
     *
     * @return the open records
     */
    public List<AttendanceRecord> findInside() {
        return attendance.findOpen();
    }

    /**
     * @param memberId the member to look up
     * @return whether the member has an open visit
     */
    public boolean isInside(long memberId) {
        return attendance.findOpenByMemberId(memberId).isPresent();
    }

    /**
     * Returns every visit ever registered, for reports and analytics.
     *
     * @return the records, open and closed
     */
    public List<AttendanceRecord> findAll() {
        return attendance.findAll();
    }

    /**
     * Returns every visit of a member, from the oldest to the newest.
     *
     * @param memberId the identifier of the member
     * @return the records
     */
    public List<AttendanceRecord> findByMember(long memberId) {
        return attendance.findByMemberId(memberId);
    }

    /**
     * Counts the entries registered on a given day.
     *
     * @param date the day
     * @return the number of entries
     */
    public long countCheckInsOn(LocalDate date) {
        return attendance.findAll().stream()
                .filter(r -> r.getCheckIn().toLocalDate().equals(date))
                .count();
    }
}