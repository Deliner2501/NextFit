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
 * Entry and exit by the access code of a carnet, read with a phone camera.
 * A phone (or its browser) may send the same scan twice in a few seconds; the
 * second one gets the first answer back instead of toggling entry and exit.
 */
public class PhoneAccess {

    /** Seconds in which a repeated scan of the same code returns the previous answer. */
    public static final int REPEAT_SECONDS = 10;

    private record Cached(Instant at, AccessResult result) {
    }

    private final MemberService memberService;
    private final AccessService accessService;
    private final AccessTokens tokens;
    private final Clock clock;
    private final Map<String, Cached> recent = new HashMap<>();

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
     * Processes a scan of an access code.
     *
     * @param token the code read from the QR
     * @return what to show on the phone and on the kiosk
     */
    public synchronized AccessResult scan(String token) {
        Optional<String> clean = AccessTokens.normalize(token);
        Instant now = clock.instant();
        if (clean.isPresent()) {
            Cached cached = recent.get(clean.get());
            if (cached != null && Duration.between(cached.at(), now).getSeconds() < REPEAT_SECONDS) {
                return cached.result();
            }
        }
        Optional<Member> member = findMember(token);
        if (member.isEmpty()) {
            return accessService.deny("Código no válido. Pide un carnet nuevo en recepción.");
        }
        AccessResult result = accessService.processMember(member.get());
        if (result.outcome() != Outcome.DENIED) {
            recent.put(clean.orElseThrow(), new Cached(now, result));
        }
        return result;
    }
}
