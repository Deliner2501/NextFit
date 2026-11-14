package com.nextfit.service;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult.Outcome;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Entry and exit by the short-lived access code that the digital carnet of the member shows, read with
 * a phone camera. The code changes every few seconds and works once: a phone (or its browser) may send
 * the same scan twice in a few seconds and gets the first answer back, but the same code later is
 * refused, so a copied picture of it is of no use.
 */
public class PhoneAccess {

    /** Seconds in which a repeated scan of the same code returns the previous answer. */
    public static final int REPEAT_SECONDS = 10;

    private record Cached(String code, Instant at, AccessResult result) {
    }

    private final MemberService memberService;
    private final AccessService accessService;
    private final AccessTokens tokens;
    private final Clock clock;
    private final Map<Long, Cached> recent = new HashMap<>();

    /**
     * Creates the service.
     *
     * @param memberService the source of members
     * @param accessService records the entry or the exit
     * @param tokens        checks the access codes
     * @param clock         tells the current time
     */
    public PhoneAccess(MemberService memberService, AccessService accessService, AccessTokens tokens, Clock clock) {
        this.memberService = memberService;
        this.accessService = accessService;
        this.tokens = tokens;
        this.clock = clock;
    }

    /**
     * Returns the access code of a member, to build the carnet QR.
     *
     * @param member the member
     * @return the code
     */
    public String tokenOf(Member member) {
        return tokens.tokenFor(member.getDocumentNumber());
    }

    /**
     * Finds the member that owns an access code.
     *
     * @param token the code read from the QR
     * @return the member, or empty when the code belongs to nobody
     */
    public Optional<Member> findMember(String token) {
        if (AccessTokens.normalize(token).isEmpty()) {
            return Optional.empty();
        }
        return memberService.findAll().stream()
                .filter(m -> tokens.matches(m.getDocumentNumber(), token))
                .findFirst();
    }

    /**
     * Returns the short-lived access code of a member, to show in the digital carnet.
     *
     * @param member the member
     * @return the code, valid for a short time
     */
    public String liveTokenOf(Member member) {
        return tokens.liveTokenFor(member.getDocumentNumber(), clock.instant());
    }

    /**
     * Finds the member that owns a short-lived access code.
     *
     * @param token the code read from the QR
     * @return the member, or empty when the code is wrong or has expired
     */
    public Optional<Member> findMemberByLiveToken(String token) {
        if (AccessTokens.normalize(token).isEmpty()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        return memberService.findAll().stream()
                .filter(m -> tokens.matchesLive(m.getDocumentNumber(), token, now))
                .findFirst();
    }

    /**
     * Processes a scan of a short-lived access code.
     *
     * @param token the code read from the QR
     * @return what to show on the phone and on the kiosk
     */
    public synchronized AccessResult scan(String token) {
        Optional<String> clean = AccessTokens.normalize(token);
        Instant now = clock.instant();
        Optional<Member> member = findMemberByLiveToken(token);
        if (member.isEmpty()) {
            if (findMember(token).isPresent()) {
                return accessService.deny("Este código ya no sirve. Abre tu carnet digital en el celular "
                        + "y muestra el código que se actualiza solo.");
            }
            return accessService.deny("Código no válido o vencido. Abre tu carnet digital e inténtalo de nuevo.");
        }
        Cached last = recent.get(member.get().getId());
        if (last != null && last.code().equals(clean.orElseThrow())) {
            if (Duration.between(last.at(), now).getSeconds() < REPEAT_SECONDS) {
                return last.result();
            }
            return accessService.deny("Este código ya se usó. Espera a que se actualice y muestra el nuevo.");
        }
        AccessResult result = accessService.processMember(member.get());
        if (result.outcome() != Outcome.DENIED) {
            recent.put(member.get().getId(), new Cached(clean.get(), now, result));
        }
        return result;
    }
}
