package com.nison.freedeck.system.network;

import androidx.annotation.Nullable;

/** A snapshot of the device's network connection. Numeric fields are {@link #NO_VALUE} when unknown. */
public final class NetworkState {

    public static final int NO_VALUE = Integer.MIN_VALUE;

    public final NetworkStatus status;
    public final boolean onWifi;
    /** The Wi-Fi network name, or null when unknown or hidden by the system (needs location permission). */
    @Nullable
    public final String ssid;
    public final int signalDbm;
    public final int frequencyMhz;
    @Nullable
    public final String ipAddress;

    public NetworkState(NetworkStatus status, boolean onWifi, @Nullable String ssid, int signalDbm,
                        int frequencyMhz, @Nullable String ipAddress) {
        this.status = status;
        this.onWifi = onWifi;
        this.ssid = ssid;
        this.signalDbm = signalDbm;
        this.frequencyMhz = frequencyMhz;
        this.ipAddress = ipAddress;
    }

    public static NetworkState disconnected() {
        return new NetworkState(NetworkStatus.DISCONNECTED, false, null, NO_VALUE, NO_VALUE, null);
    }

    public boolean isConnected() {
        return status != NetworkStatus.DISCONNECTED;
    }
}
