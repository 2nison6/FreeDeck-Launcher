package com.nison.freedeck.ui.sound;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayThrottleTest {

    @Test
    public void theFirstSoundAlwaysPlays() {
        assertTrue(new PlayThrottle(100).tryPlay(0));
    }

    @Test
    public void aSecondSoundTooSoonIsHeldBack() {
        PlayThrottle throttle = new PlayThrottle(100);
        assertTrue(throttle.tryPlay(1000));
        assertFalse(throttle.tryPlay(1050));
    }

    @Test
    public void aSoundAfterTheGapPlays() {
        PlayThrottle throttle = new PlayThrottle(100);
        assertTrue(throttle.tryPlay(1000));
        assertTrue(throttle.tryPlay(1100));
    }

    @Test
    public void aHeldBackSoundDoesNotExtendTheWait() {
        PlayThrottle throttle = new PlayThrottle(100);
        assertTrue(throttle.tryPlay(1000));
        assertFalse(throttle.tryPlay(1060));
        assertTrue(throttle.tryPlay(1100)); // 100 ms after the last sound that actually played
    }

    @Test
    public void aKeyHeldDownPlaysOnlyAtTheRateLimit() {
        PlayThrottle throttle = new PlayThrottle(100);
        int played = 0;
        for (long t = 0; t < 1000; t += 20) { // a card every 20 ms for a second
            if (throttle.tryPlay(t)) {
                played++;
            }
        }
        assertTrue("played " + played, played == 10);
    }

    @Test
    public void aZeroGapNeverHoldsAnythingBack() {
        PlayThrottle throttle = new PlayThrottle(0);
        assertTrue(throttle.tryPlay(5));
        assertTrue(throttle.tryPlay(5));
    }
}
