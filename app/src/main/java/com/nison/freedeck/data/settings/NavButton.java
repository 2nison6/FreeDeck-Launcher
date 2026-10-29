package com.nison.freedeck.data.settings;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** The icon buttons at the right of the navbar. */
public enum NavButton {
    HOME("home", R.string.nav_home),
    APPS("apps", R.string.nav_apps),
    SETTINGS("settings", R.string.nav_settings),
    WIFI("wifi", R.string.nav_wifi);

    public final String id;
    @StringRes
    public final int label;

    NavButton(String id, @StringRes int label) {
        this.id = id;
        this.label = label;
    }
}
