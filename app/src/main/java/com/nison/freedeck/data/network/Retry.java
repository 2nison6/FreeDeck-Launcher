package com.nison.freedeck.data.network;

import java.util.function.Supplier;

/** Tries something again and again until it works or the time runs out. Blocking: call from a background thread. */
public final class Retry {

    /** Time and waiting, so the logic can be tested without really waiting. */
    public interface Clock {
        long now();

        void sleep(long millis) throws InterruptedException;
    }

    public static final Clock SYSTEM = new Clock() {
        @Override
        public long now() {
            return System.currentTimeMillis();
        }

        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    };

    private Retry() {
    }

    /**
     * Calls {@code attempt} until it returns something other than null, pausing {@code pauseMs} between tries.
     * Always tries at least once, and does not start another try once {@code budgetMs} has passed.
     *
     * @return the first result, or null if every try failed or the thread was interrupted
     */
    public static <T> T until(Supplier<T> attempt, long budgetMs, long pauseMs, Clock clock) {
        long deadline = clock.now() + budgetMs;
        while (true) {
            T result = attempt.get();
            if (result != null) {
                return result;
            }
            if (clock.now() + pauseMs >= deadline) {
                return null;
            }
            try {
                clock.sleep(pauseMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }
}
