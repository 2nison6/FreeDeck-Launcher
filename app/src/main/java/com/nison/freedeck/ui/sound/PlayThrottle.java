package com.nison.freedeck.ui.sound;

/** Lets a sound through at most once per interval, so holding a key doesn't turn it into a buzz. */
public final class PlayThrottle {

    private final long minGapMs;
    private long lastPlayedAt;
    private boolean hasPlayed;

    public PlayThrottle(long minGapMs) {
        this.minGapMs = minGapMs;
    }

    /** @return true if the sound may play now; this counts as playing it */
    public boolean tryPlay(long nowMs) {
        if (hasPlayed && nowMs - lastPlayedAt < minGapMs) {
            return false;
        }
        hasPlayed = true;
        lastPlayedAt = nowMs;
        return true;
    }
}
