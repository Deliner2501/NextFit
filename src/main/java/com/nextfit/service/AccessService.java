package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Member;
import com.nextfit.service.AccessResult.Outcome;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Turns a scanned carnet into an entry or an exit. A member who is outside
 * enters (if the rules allow it); a member who is inside leaves. It never
 * throws for a rejected scan: the reason comes back in the {@link AccessResult}
 * so the kiosk can show it.
 */
public class AccessService {

    private final MemberService memberService;
    private final AttendanceService attendanceService;
    private final MembershipExpiry expiry;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param memberService     used to find the member by document
     * @param attendanceService records the entry and the exit
     * @param expiry            tells when the membership ends
     * @param clock             the clock that tells today
     */
    public AccessService(MemberService memberService, AttendanceService attendanceService,
                         MembershipExpiry expiry, Clock clock) {
        this.memberService = memberService;
        this.attendanceService = attendanceService;
        this.expiry = expiry;
        this.clock = clock;
    }

    /**
     * Processes a scanned or typed code.
     *
     * @param scanned the raw text from the scanner
     * @return the result to show
     */
    public AccessResult process(String scanned) {
        Optional<String> document = CarnetCode.decode(scanned);
        if (document.isEmpty()) {
            return denied(null, "Código vacío. Escanea el carnet o escribe el documento.");
        }
        Optional<Member> found = memberService.search(document.get()).stream()
                .filter(m -> m.getDocumentNumber().equalsIgnoreCase(document.get()))
                .findFirst();
        if (found.isEmpty()) {
            return denied(null, "Carnet no reconocido: " + document.get());
        }
        Member member = found.get();
        boolean inside = attendanceService.findInside().stream()
                .anyMatch(r -> r.getMemberId() == member.getId());
        if (inside) {
            attendanceService.checkOut(member.getId());
            return granted(Outcome.EXIT, member, "Hasta pronto, " + firstName(member));
        }
        try {
            attendanceService.checkIn(member.getId());
        } catch (BusinessRuleException e) {
            return denied(member, e.getMessage());
        }
        return granted(Outcome.ENTRY, member, "Bienvenido, " + firstName(member));
    }

    private AccessResult granted(Outcome outcome, Member member, String message) {
        Optional<LocalDate> until = expiry.validUntil(member.getId());
        long days = until.map(d -> ChronoUnit.DAYS.between(LocalDate.now(clock), d)).orElse(-1L);
        return new AccessResult(outcome, member, message, until.orElse(null), days);
    }

    private AccessResult denied(Member member, String message) {
        return new AccessResult(Outcome.DENIED, member, message, null, -1);
    }

    private static String firstName(Member member) {
        String name = member.getFullName().trim();
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }
}
