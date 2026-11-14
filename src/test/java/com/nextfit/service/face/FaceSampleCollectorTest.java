package com.nextfit.service.face;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static com.nextfit.service.face.FaceTestData.face;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceSampleCollectorTest {

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
    private final FaceSampleCollector collector = new FaceSampleCollector(clock);

    private static FaceFrame frame(double yaw) {
        return new FaceFrame(new byte[0], face(1, 0), yaw);
    }

    @Test
    void keepsThreeSpacedFrontalSamples() {
        assertTrue(collector.offer(frame(0.1)));
        assertFalse(collector.offer(frame(0.1))); // too soon after the last one
        clock.advance(Duration.ofSeconds(1));
        assertTrue(collector.offer(frame(-0.1)));
        clock.advance(Duration.ofSeconds(1));
        assertFalse(collector.complete());
        assertTrue(collector.offer(frame(0)));
        assertTrue(collector.complete());
        assertEquals(3, collector.samples().size());
        clock.advance(Duration.ofSeconds(1));
        assertFalse(collector.offer(frame(0))); // already complete
    }

    @Test
    void rejectsFacesLookingToTheSideOrNoFace() {
        assertFalse(collector.offer(frame(0.5)));
        assertFalse(collector.offer(new FaceFrame(new byte[0], null, 0)));
        assertEquals(0, collector.count());
    }

    @Test
    void hintsGuideThePerson() {
        assertTrue(collector.hint(new FaceFrame(new byte[0], null, 0)).contains("rostro"));
        assertTrue(collector.hint(frame(0.6)).contains("de frente"));
        assertTrue(collector.hint(frame(0)).contains("1 de 3"));
    }
}
