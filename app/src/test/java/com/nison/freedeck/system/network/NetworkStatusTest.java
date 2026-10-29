package com.nison.freedeck.system.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NetworkStatusTest {

    @Test
    public void disconnectedWhenThereIsNoNetwork() {
        assertEquals(NetworkStatus.DISCONNECTED, NetworkStatus.classify(false, false));
    }

    @Test
    public void disconnectedWinsOverInternetFlag() {
        assertEquals(NetworkStatus.DISCONNECTED, NetworkStatus.classify(false, true));
    }

    @Test
    public void connectedWithInternet() {
        assertEquals(NetworkStatus.CONNECTED, NetworkStatus.classify(true, true));
    }

    @Test
    public void noInternetWhenJoinedButNotOnline() {
        assertEquals(NetworkStatus.NO_INTERNET, NetworkStatus.classify(true, false));
    }

    @Test
    public void colorsFollowConnectivity() {
        assertEquals(Severity.GOOD, NetworkStatus.CONNECTED.severity);
        assertEquals(Severity.WARNING, NetworkStatus.NO_INTERNET.severity);
        assertEquals(Severity.CRITICAL, NetworkStatus.DISCONNECTED.severity);
    }
}
