package com.nison.freedeck.data.settings;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** The pieces of information the screensaver can show over its background. */
public enum ScreensaverWidget {
    GREETING("greeting", R.string.screensaver_widget_greeting, true),
    CLOCK("clock", R.string.screensaver_widget_clock, true),
    DATE("date", R.string.screensaver_widget_date, true),
    LOCATION("location", R.string.screensaver_widget_location, false),
    WEATHER("weather", R.string.screensaver_widget_weather, false);

    /** Stable key for saving the choice. */
    public final String id;
    @StringRes
    public final int label;
    /** Whether it shows until the user turns it off. */
    public final boolean shownByDefault;

    ScreensaverWidget(String id, @StringRes int label, boolean shownByDefault) {
        this.id = id;
        this.label = label;
        this.shownByDefault = shownByDefault;
    }
}
