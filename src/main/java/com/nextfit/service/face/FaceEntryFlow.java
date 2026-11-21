package com.nextfit.service.face;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

/**
 * The steps to enter with the face, as a small state machine fed with camera frames:
 * first the face is recognized (several frames in a row), then the person turns the head to both sides
 * (proof of a live person) and finally the same face is confirmed again. Only then the caller lets the
 * member in. Any other face, a long silence or a refused challenge starts over.
 */
public final class FaceEntryFlow {

    /** Where the person is in the process. */
    public enum Phase { LOOK, TURN, CONFIRM, DONE }

    /**
     * What the screen must show after one frame.
     *
     * @param phase    the current step
     * @param hint     a short instruction for the person
     * @param memberId the member to let in, only in {@link Phase#DONE}; otherwise null
     * @param unknown  true once when a face nobody knows has been in front of the camera for a while
     */
    public record Step(Phase phase, String hint, Long memberId, boolean unknown) {
    }

    public static final int CONFIRMATIONS = 3;
    public static final Duration TURN_TIME = Duration.ofSeconds(12);
    public static final Duration COOLDOWN = Duration.ofSeconds(8);
    static final double TURN_ANGLE = 0.25;
    static final int UNKNOWN_FRAMES = 10;

    private final Function<FaceFrame, Optional<Long>> matcher;
    private final Clock clock;
    private final HeadTurnChallenge challenge = new HeadTurnChallenge(TURN_ANGLE);
    private Phase phase = Phase.LOOK;
    private Long candidate;
    private int streak;
    private int unknownFrames;
    private Instant turnStart;
    private Instant pausedUntil = Instant.MIN;

    /**
     * @param matcher finds the member of a frontal face (see {@link FaceAccessService#match})
     * @param clock   the clock
     */
    public FaceEntryFlow(Function<FaceFrame, Optional<Long>> matcher, Clock clock) {
        this.matcher = matcher;
        this.clock = clock;
    }

    /** @return the current step */
    public Phase phase() {
        return phase;
    }

    /**
     * Processes one frame of the camera.
     *
     * @param frame what the camera saw
     * @return what to show
     */
    public Step feed(FaceFrame frame) {
        Instant now = clock.instant();
        if (now.isBefore(pausedUntil)) {
            return new Step(Phase.LOOK, "Listo. Siguiente persona...", null, false);
        }
        return switch (phase) {
            case LOOK -> look(frame);
            case TURN -> turn(frame, now);
            case CONFIRM -> confirm(frame);
            case DONE -> restart("Mira a la cámara");
        };
    }

    /** Starts again and ignores the camera for a few seconds (after someone entered or was refused). */
    public void pause() {
        restart("");
        pausedUntil = clock.instant().plus(COOLDOWN);
    }

    private Step look(FaceFrame frame) {
        if (!frame.hasFace()) {
            streak = 0;
            unknownFrames = 0;
            return step(Phase.LOOK, "Mira a la cámara");
        }
        if (!frame.isFrontal()) {
            streak = 0;
            return step(Phase.LOOK, "Mira de frente a la cámara");
        }
        Optional<Long> id = matcher.apply(frame);
        if (id.isEmpty()) {
            streak = 0;
            unknownFrames++;
            if (unknownFrames == UNKNOWN_FRAMES) {
                unknownFrames = 0;
                pausedUntil = clock.instant().plus(COOLDOWN);
                return new Step(Phase.LOOK, FaceAccessService.UNKNOWN_MESSAGE, null, true);
            }
            return step(Phase.LOOK, "Reconociendo...");
        }
        unknownFrames = 0;
        if (id.get().equals(candidate)) {
            streak++;
        } else {
            candidate = id.get();
            streak = 1;
        }
        if (streak >= CONFIRMATIONS) {
            phase = Phase.TURN;
            turnStart = clock.instant();
            streak = 0;
            return step(Phase.TURN, "Gira la cabeza a la izquierda y luego a la derecha");
        }
        return step(Phase.LOOK, "Reconociendo...");
    }

    private Step turn(FaceFrame frame, Instant now) {
        if (now.isAfter(turnStart.plus(TURN_TIME))) {
            return restart("Tiempo agotado. Mira a la cámara e inténtalo de nuevo");
        }
        if (!frame.hasFace()) {
            return step(Phase.TURN, "Gira la cabeza a la izquierda y luego a la derecha");
        }
        if (challenge.feed(frame.yaw())) {
            phase = Phase.CONFIRM;
            streak = 0;
            return step(Phase.CONFIRM, "Ahora mira de frente a la cámara");
        }
        return step(Phase.TURN, "Gira la cabeza a la izquierda y luego a la derecha");
    }

    private Step confirm(FaceFrame frame) {
        if (!frame.isFrontal()) {
            return step(Phase.CONFIRM, "Ahora mira de frente a la cámara");
        }
        Optional<Long> id = matcher.apply(frame);
        if (id.isEmpty() || !id.get().equals(candidate)) {
            return restart("No coincide. Mira a la cámara e inténtalo de nuevo");
        }
        streak++;
        if (streak >= CONFIRMATIONS) {
            Long who = candidate;
            phase = Phase.DONE;
            return new Step(Phase.DONE, "Verificado", who, false);
        }
        return step(Phase.CONFIRM, "Ahora mira de frente a la cámara");
    }

    private Step restart(String hint) {
        phase = Phase.LOOK;
        candidate = null;
        streak = 0;
        unknownFrames = 0;
        challenge.reset();
        return step(Phase.LOOK, hint);
    }

    private Step step(Phase p, String hint) {
        return new Step(p, hint, null, false);
    }
}
