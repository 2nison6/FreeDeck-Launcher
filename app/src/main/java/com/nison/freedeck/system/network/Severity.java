package com.nison.freedeck.system.network;

import androidx.annotation.ColorRes;

import com.nison.freedeck.R;

/** How good a connection property is, with the green / orange / red tint that shows it. */
public enum Severity {
    GOOD(R.color.wifi_good),
    WARNING(R.color.wifi_warning),
    CRITICAL(R.color.wifi_critical);

    @ColorRes
    public final int tint;

    Severity(@ColorRes int tint) {
        this.tint = tint;
    }
}
