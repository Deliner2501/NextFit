package com.nextfit.service.face;

import com.nextfit.service.face.FaceEntryFlow.Phase;
import com.nextfit.service.face.FaceEntryFlow.Step;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static com.nextfit.service.face.FaceTestData.face;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceEntryFlowTest {

    /** A clock the test moves by hand. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-14T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    // The vector decides who it is: first value 1 means member 1, 2 means member 2, anything else unknown.
    private final FaceEntryFlow flow = new FaceEntryFlow(frame -> {
        float who = frame.vector()[0];
        return who == 1 || who == 2 ? Optional.of((long) who) : Optional.empty();
    }, clock);

    private static FaceFrame frame(float who, double yaw) {
        return new FaceFrame(new byte[0], face(who, 0), yaw);
    }

    private Step feed(float who, double yaw, int times) {
        Step last = null;
        for (int i = 0; i < times; i++) {
            last = flow.feed(frame(who, yaw));
        }
        return last;
    }

    private void turnHead() {
        flow.feed(frame(1, -0.5));
        flow.feed(frame(1, 0.5));
    }

    @Test
    void completeEntryNeedsRecognitionTurnAndConfirmation() {
        assertEquals(Phase.LOOK, feed(1, 0, 2).phase());
        assertEquals(Phase.TURN, feed(1, 0, 1).phase());
        assertEquals(Phase.TURN, flow.feed(frame(1, -0.5)).phase());
        assertEquals(Phase.CONFIRM, flow.feed(frame(1, 0.5)).phase());
        assertEquals(Phase.CONFIRM, feed(1, 0, 2).phase());
        Step done = feed(1, 0, 1);
        assertEquals(Phase.DONE, done.phase());
        assertEquals(1L, done.memberId());
    }

    @Test
    void turningOnlyToOneSideDoesNotPass() {
        feed(1, 0, 3);
        assertEquals(Phase.TURN, feed(1, -0.6, 10).phase());
    }

    @Test
    void anotherFaceAfterTheTurnStartsOver() {
        feed(1, 0, 3);
        turnHead();
        Step step = flow.feed(frame(2, 0));
        assertEquals(Phase.LOOK, step.phase());
        assertNull(step.memberId());
    }

    @Test
    void challengeExpires() {
        feed(1, 0, 3);
        clock.advance(FaceEntryFlow.TURN_TIME.plusSeconds(1));
        assertEquals(Phase.LOOK, flow.feed(frame(1, 0)).phase());
    }

    @Test
    void sideFaceDoesNotCountAsRecognition() {
        assertEquals(Phase.LOOK, feed(1, 0.6, 10).phase());
    }

    @Test
    void unknownFaceIsReportedOnceThenPaused() {
        Step last = feed(9, 0, FaceEntryFlow.UNKNOWN_FRAMES);
        assertTrue(last.unknown());
        assertFalse(feed(9, 0, 5).unknown());
    }

    @Test
    void pauseIgnoresTheCameraForAWhile() {
        flow.pause();
        assertEquals(Phase.LOOK, feed(1, 0, 5).phase());
        clock.advance(FaceEntryFlow.COOLDOWN.plusSeconds(1));
        assertEquals(Phase.TURN, feed(1, 0, 3).phase());
    }
}
