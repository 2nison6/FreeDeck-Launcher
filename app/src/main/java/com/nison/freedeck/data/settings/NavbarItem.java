package com.nison.freedeck.data.settings;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/**
 * The pieces of the navbar that can be shown or hidden. Out of the box the navbar shows the greeting and time on
 * one line and the date under them; location and weather are off until switched on in Settings.
 */
public enum NavbarItem {
    GREETING("greeting", R.string.navbar_item_greeting, true),
    TIME("time", R.string.navbar_item_time, true),
    DATE("date", R.string.navbar_item_date, true),
    LOCATION("location", R.string.navbar_item_location, false),
    WEATHER("weather", R.string.navbar_item_weather, false);

    /** How many pieces (in the saved order) go on the first line; the rest go on the second. */
    public static final int FIRST_LINE_COUNT = 2;

    /** Stable key for saving the choice. */
    public final String id;
    @StringRes
    public final int label;
    /** Whether it is shown until the user chooses otherwise. */
    public final boolean shownByDefault;

    NavbarItem(String id, @StringRes int label, boolean shownByDefault) {
        this.id = id;
        this.label = label;
        this.shownByDefault = shownByDefault;
    }
}
