package com.nison.freedeck.system.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NetworkStateTest {

    private static NetworkState wifi(NetworkStatus status, int dbm) {
        return new NetworkState(status, true, "Home", dbm, 5180, "192.168.0.2");
    }

    @Test
    public void disconnectedStateIsNotConnected() {
        NetworkState state = NetworkState.disconnected();
        assertFalse(state.isConnected());
        assertEquals(Severity.CRITICAL, state.status.severity);
    }

    @Test
    public void onlineStaysGreenEvenWithAWeakSignal() {
        NetworkState weak = wifi(NetworkStatus.CONNECTED, -88);
        assertEquals(Severity.GOOD, weak.status.severity);
        assertEquals(Severity.CRITICAL, SignalQuality.fromDbm(weak.signalDbm).severity); // the signal row has its own color
    }

    @Test
    public void joinedWithoutInternetIsOrangeEvenWithAStrongSignal() {
        assertEquals(Severity.WARNING, wifi(NetworkStatus.NO_INTERNET, -40).status.severity);
    }

    @Test
    public void wiredConnectionIsConnectedWithNoSignal() {
        NetworkState wired = new NetworkState(NetworkStatus.CONNECTED, false, null,
                NetworkState.NO_VALUE, NetworkState.NO_VALUE, "192.168.0.9");
        assertTrue(wired.isConnected());
        assertEquals(Severity.GOOD, wired.status.severity);
    }
}
