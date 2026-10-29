package com.nison.freedeck.data.settings;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** How long the launcher sits untouched before the screensaver starts. */
public enum ScreensaverDelay {
    SECONDS_30("30s", 30, R.string.screensaver_delay_30s),
    MINUTES_1("1", 60, R.string.screensaver_delay_1),
    MINUTES_2("2", 120, R.string.screensaver_delay_2),
    MINUTES_5("5", 300, R.string.screensaver_delay_5),
    MINUTES_10("10", 600, R.string.screensaver_delay_10);

    public static final ScreensaverDelay DEFAULT = MINUTES_2;

    /** Stable key for saving the choice. */
    public final String id;
    public final int seconds;
    @StringRes
    public final int label;

    ScreensaverDelay(String id, int seconds, @StringRes int label) {
        this.id = id;
        this.seconds = seconds;
        this.label = label;
    }

    public long millis() {
        return seconds * 1000L;
    }

    /** The delay saved under {@code id}; the default when there is none or it is not recognised. */
    public static ScreensaverDelay fromId(String id) {
        for (ScreensaverDelay delay : values()) {
            if (delay.id.equals(id)) {
                return delay;
            }
        }
        return DEFAULT;
    }

    public ScreensaverDelay next() {
        ScreensaverDelay[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
