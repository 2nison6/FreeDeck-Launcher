package com.nison.freedeck.data.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RetryTest {

    /** A clock that only moves when asked to sleep. */
    private static final class FakeClock implements Retry.Clock {
        long now;
        boolean interrupt;

        @Override
        public long now() {
            return now;
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            if (interrupt) {
                throw new InterruptedException();
            }
            now += millis;
        }
    }

    @Test
    public void returnsTheFirstResultWithoutWaiting() {
        FakeClock clock = new FakeClock();
        assertEquals("ok", Retry.until(() -> "ok", 10_000, 1_000, clock));
        assertEquals(0, clock.now);
    }

    @Test
    public void triesAgainUntilItWorks() {
        FakeClock clock = new FakeClock();
        int[] calls = {0};
        String result = Retry.until(() -> ++calls[0] < 4 ? null : "ok", 10_000, 1_000, clock);
        assertEquals("ok", result);
        assertEquals(4, calls[0]);
        assertEquals(3_000, clock.now);
    }

    @Test
    public void givesUpWhenTheTimeIsUp() {
        FakeClock clock = new FakeClock();
        int[] calls = {0};
        assertNull(Retry.until(() -> {
            calls[0]++;
            return null;
        }, 5_000, 1_000, clock));
        assertTrue(clock.now <= 5_000);
        assertEquals(5, calls[0]);
    }

    @Test
    public void alwaysTriesAtLeastOnce() {
        FakeClock clock = new FakeClock();
        int[] calls = {0};
        assertNull(Retry.until(() -> {
            calls[0]++;
            return null;
        }, 0, 1_000, clock));
        assertEquals(1, calls[0]);
    }

    @Test
    public void stopsWhenInterrupted() {
        FakeClock clock = new FakeClock();
        clock.interrupt = true;
        assertNull(Retry.until(() -> null, 10_000, 1_000, clock));
        assertTrue(Thread.interrupted()); // the flag is put back; clear it so other tests are not affected
    }
}
