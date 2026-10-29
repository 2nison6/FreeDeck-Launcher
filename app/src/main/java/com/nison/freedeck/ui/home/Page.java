package com.nison.freedeck.ui.home;

/** A page the home screen can show below the nav bar. */
public enum Page {
    HOME,
    APPS,
    SETTINGS,
    WIFI;

    /** The page shown on launch and whenever the Home button is pressed. */
    public static final Page DEFAULT = HOME;
}
