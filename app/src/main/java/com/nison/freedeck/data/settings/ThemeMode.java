package com.nison.freedeck.data.settings;

import android.content.res.Configuration;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** Whether the launcher is dark or light. Dark is the default. */
public enum ThemeMode {
    DARK("dark", R.string.theme_dark, Configuration.UI_MODE_NIGHT_YES),
    LIGHT("light", R.string.theme_light, Configuration.UI_MODE_NIGHT_NO);

    public static final ThemeMode DEFAULT = DARK;

    /** Stable key for saving the choice. */
    public final String id;
    @StringRes
    public final int label;
    /** The night-mode bits of the screen configuration that make resources pick this mode's colours. */
    public final int uiNightMode;

    ThemeMode(String id, @StringRes int label, int uiNightMode) {
        this.id = id;
        this.label = label;
        this.uiNightMode = uiNightMode;
    }

    /** The mode saved under {@code id}; the default when there is none or it is not recognised. */
    public static ThemeMode fromId(String id) {
        for (ThemeMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return DEFAULT;
    }
}
