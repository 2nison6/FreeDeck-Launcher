package com.nison.freedeck.system.network;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** Whether the device is online, independent of how strong the Wi-Fi signal is. */
public enum NetworkStatus {
    CONNECTED(Severity.GOOD, R.string.network_connected),
    /** Joined to a network, but the internet is not reachable through it. */
    NO_INTERNET(Severity.WARNING, R.string.network_no_internet),
    DISCONNECTED(Severity.CRITICAL, R.string.network_disconnected);

    public final Severity severity;
    @StringRes
    public final int description;

    NetworkStatus(Severity severity, @StringRes int description) {
        this.severity = severity;
        this.description = description;
    }

    public static NetworkStatus classify(boolean connected, boolean hasInternet) {
        if (!connected) {
            return DISCONNECTED;
        }
        return hasInternet ? CONNECTED : NO_INTERNET;
    }
}
