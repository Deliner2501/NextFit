package com.nextfit.service.face;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadTurnChallengeTest {

    @Test
    void aStillFaceNeverPasses() {
        HeadTurnChallenge challenge = new HeadTurnChallenge(0.2);
        for (int i = 0; i < 100; i++) {
            assertFalse(challenge.feed(0.02));
        }
    }

    @Test
    void turningToOneSideOnlyIsNotEnough() {
        HeadTurnChallenge challenge = new HeadTurnChallenge(0.2);
        assertFalse(challenge.feed(-0.3));
        assertFalse(challenge.feed(-0.4));
        assertFalse(challenge.passed());
    }

    @Test
    void turningToBothSidesPassesInAnyOrder() {
        HeadTurnChallenge first = new HeadTurnChallenge(0.2);
        first.feed(-0.3);
        first.feed(0.0);
        assertTrue(first.feed(0.3));

        HeadTurnChallenge second = new HeadTurnChallenge(0.2);
        second.feed(0.25);
        assertTrue(second.feed(-0.25));
    }

    @Test
    void resetStartsAgain() {
        HeadTurnChallenge challenge = new HeadTurnChallenge(0.2);
        challenge.feed(-0.3);
        challenge.feed(0.3);
        challenge.reset();
        assertFalse(challenge.passed());
    }
}
