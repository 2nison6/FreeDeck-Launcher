package com.nison.freedeck.data.settings;

/** A background (the launcher's or the screensaver's) can cycle by itself through the built-in ones, or the uploaded ones. */
public enum BackgroundRandom {
    OFF(null),
    SYSTEM("random_system"),
    CUSTOM("random_custom");

    /** Stable key for saving the choice; null when the screensaver uses one fixed background. */
    public final String id;

    BackgroundRandom(String id) {
        this.id = id;
    }

    /** The mode saved under {@code id}. The older single "random" meant the built-in backgrounds. */
    public static BackgroundRandom fromId(String id) {
        if ("random".equals(id)) {
            return SYSTEM;
        }
        for (BackgroundRandom mode : values()) {
            if (mode.id != null && mode.id.equals(id)) {
                return mode;
            }
        }
        return OFF;
    }
}
