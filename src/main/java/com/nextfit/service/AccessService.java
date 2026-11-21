package com.nextfit.service;

import com.nextfit.exceptions.BusinessRuleException;
import com.nextfit.model.Member;
import com.nextfit.service.AccessResult.Outcome;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Turns a scanned carnet into an entry or an exit. A member who is outside
 * enters (if the rules allow it); a member who is inside leaves. It never
 * throws for a rejected scan: the reason comes back in the {@link AccessResult}
 * so the kiosk can show it. Every result, whatever scanned it (USB reader,
 * typed document or a phone), is also announced to the registered listeners,
 * so the kiosk screen reacts to scans that did not happen on its own text box.
 */
public class AccessService {

    private final MemberService memberService;
    private final AttendanceService attendanceService;
    private final MembershipExpiry expiry;
    private final Clock clock;
    private final List<Consumer<AccessResult>> listeners = new CopyOnWriteArrayList<>();

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
     * Registers an action to run after every result. It may run on any thread.
     *
     * @param listener the action
     */
    public void addListener(Consumer<AccessResult> listener) {
        listeners.add(listener);
    }

    /**
     * Processes a scanned or typed code (the carnet text or a document number).
     *
     * @param scanned the raw text from the scanner
     * @return the result to show
     */
    public AccessResult process(String scanned) {
        Optional<String> document = CarnetCode.decode(scanned);
        if (document.isEmpty()) {
            return deny("Código vacío. Escanea el carnet o escribe el documento.");
        }
        Optional<Member> found = memberService.search(document.get()).stream()
                .filter(m -> m.getDocumentNumber().equalsIgnoreCase(document.get()))
                .findFirst();
        if (found.isEmpty()) {
            return deny("Carnet no reconocido: " + document.get());
        }
        return processMember(found.get());
    }

    /**
     * Records the entry or the exit of a member already identified (for example
     * by the access code of a phone scan).
     *
     * @param member the member
     * @return the result to show
     */
    public synchronized AccessResult processMember(Member member) {
        boolean inside = attendanceService.findInside().stream()
                .anyMatch(r -> r.getMemberId() == member.getId());
        if (inside) {
            attendanceService.checkOut(member.getId());
            return publish(granted(Outcome.EXIT, member, "Hasta pronto, " + firstName(member)));
        }
        try {
            attendanceService.checkIn(member.getId());
        } catch (BusinessRuleException e) {
            return publish(new AccessResult(Outcome.DENIED, member, e.getMessage(), null, -1));
        }
        return publish(granted(Outcome.ENTRY, member, "Bienvenido, " + firstName(member)));
    }

    /**
     * Builds and announces a denial that is not about a known member (blank or
     * unknown code).
     *
     * @param message the reason to show
     * @return the denied result
     */
    public AccessResult deny(String message) {
        return publish(new AccessResult(Outcome.DENIED, null, message, null, -1));
    }

    private AccessResult publish(AccessResult result) {
        for (Consumer<AccessResult> listener : listeners) {
            listener.accept(result);
        }
        return result;
    }

    private AccessResult granted(Outcome outcome, Member member, String message) {
        Optional<LocalDate> until = expiry.validUntil(member.getId());
        long days = until.map(d -> ChronoUnit.DAYS.between(LocalDate.now(clock), d)).orElse(-1L);
        return new AccessResult(outcome, member, message, until.orElse(null), days);
    }

    private static String firstName(Member member) {
        String name = member.getFullName().trim();
        int space = name.indexOf(' ');
        return space < 0 ? name : name.substring(0, space);
    }
}
