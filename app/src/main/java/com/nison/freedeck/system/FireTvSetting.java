package com.nison.freedeck.system;

import androidx.annotation.Nullable;

/**
 * The sections of Fire TV's own Settings, each with where it lives. Plain data (no Android objects), so it can
 * be tested; {@link SystemSettings} turns it into the intents that open the screen.
 */
public enum FireTvSetting {
    /** Fire TV's Settings landing screen. The stock Settings entry just returns to Home when this app is the launcher. */
    MAIN(".tv.device.DeviceActivity", "android.settings.DEVICE_INFO_SETTINGS"),
    NETWORK(".tv.network.NetworkActivity", "android.settings.WIFI_SETTINGS");

    /** The screen's class inside Fire TV's settings app, written relative to its package (starting with a dot). */
    public final String activity;
    /** A standard Android action that reaches the same screen, tried if the direct route is refused; may be null. */
    @Nullable
    public final String action;

    FireTvSetting(String activity, @Nullable String action) {
        this.activity = activity;
        this.action = action;
    }
}
