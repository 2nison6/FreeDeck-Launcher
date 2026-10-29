package com.nison.freedeck.data.settings;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** How the navbar shows the time. 12-hour is the default. */
public enum ClockFormat {
    HOURS_12("12", R.string.clock_12_hour),
    HOURS_24("24", R.string.clock_24_hour);

    public static final ClockFormat DEFAULT = HOURS_12;

    /** Stable key for saving the choice. */
    public final String id;
    @StringRes
    public final int label;

    ClockFormat(String id, @StringRes int label) {
        this.id = id;
        this.label = label;
    }

    /** The format saved under {@code id}; the default when there is none or it is not recognised. */
    public static ClockFormat fromId(String id) {
        for (ClockFormat format : values()) {
            if (format.id.equals(id)) {
                return format;
            }
        }
        return DEFAULT;
    }
}
