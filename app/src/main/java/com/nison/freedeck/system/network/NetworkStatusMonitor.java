package com.nison.freedeck.system.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import java.net.Inet4Address;

/** Watches the default network and reports a {@link NetworkState} on the main thread whenever it changes. */
public class NetworkStatusMonitor {

    /** Receives network updates. */
    public interface Listener {
        void onNetworkStateChanged(NetworkState state);
    }

    private static final int UNKNOWN_RSSI = -127;
    private static final String UNKNOWN_SSID = "<unknown ssid>";

    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            refresh();
        }

        @Override
        public void onLost(Network network) {
            refresh();
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            refresh();
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            refresh();
        }
    };

    private final BroadcastReceiver signalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    private Listener listener;

    public NetworkStatusMonitor(Context context) {
        this.context = com.nison.freedeck.ui.common.AppContext.wrap(context.getApplicationContext());
        this.connectivityManager = this.context.getSystemService(ConnectivityManager.class);
        this.wifiManager = this.context.getSystemService(WifiManager.class);
    }

    /** Starts watching and immediately reports the current state. */
    public void start(Listener listener) {
        stop();
        this.listener = listener;
        connectivityManager.registerDefaultNetworkCallback(networkCallback, mainHandler);
        context.registerReceiver(signalReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        refresh();
    }

    public void stop() {
        if (listener == null) {
            return;
        }
        listener = null;
        connectivityManager.unregisterNetworkCallback(networkCallback);
        context.unregisterReceiver(signalReceiver);
    }

    /** Re-reads and reports the current state, e.g. after a permission was granted. */
    public void refresh() {
        if (listener != null) {
            listener.onNetworkStateChanged(currentState());
        }
    }

    private NetworkState currentState() {
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = network != null ? connectivityManager.getNetworkCapabilities(network) : null;
        if (capabilities == null) {
            return NetworkState.disconnected();
        }
        NetworkStatus status = NetworkStatus.classify(true,
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
        String ipAddress = findIpv4Address(network);

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return new NetworkState(status, false, null, NetworkState.NO_VALUE, NetworkState.NO_VALUE, ipAddress);
        }

        WifiInfo info = wifiManager.getConnectionInfo(); // Deprecated, but the only source usable from API 26.
        int rssi = info != null ? info.getRssi() : UNKNOWN_RSSI;
        return new NetworkState(
                status,
                true,
                info != null ? parseSsid(info.getSSID()) : null,
                rssi != UNKNOWN_RSSI ? rssi : NetworkState.NO_VALUE,
                info != null ? positiveOrUnknown(info.getFrequency()) : NetworkState.NO_VALUE,
                ipAddress);
    }

    private String findIpv4Address(Network network) {
        LinkProperties properties = connectivityManager.getLinkProperties(network);
        if (properties == null) {
            return null;
        }
        for (LinkAddress address : properties.getLinkAddresses()) {
            if (address.getAddress() instanceof Inet4Address) {
                return address.getAddress().getHostAddress();
            }
        }
        return null;
    }

    private static String parseSsid(String raw) {
        if (raw == null || UNKNOWN_SSID.equals(raw)) {
            return null;
        }
        String name = raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")
                ? raw.substring(1, raw.length() - 1)
                : raw;
        return name.isEmpty() ? null : name;
    }

    private static int positiveOrUnknown(int value) {
        return value > 0 ? value : NetworkState.NO_VALUE;
    }
}
