package com.nison.freedeck.data.upload;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/** Finds this TV's address on the home network, which is the one a phone on the same Wi-Fi can reach. */
public final class LocalAddress {

    private LocalAddress() {
    }

    /** @return the address as text, or null if the TV has no network address */
    public static String find() {
        try {
            for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!network.isUp() || network.isLoopback()) {
                    continue;
                }
                for (InetAddress address : Collections.list(network.getInetAddresses())) {
                    if (isUsable(address)) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (java.net.SocketException e) {
            return null;
        }
        return null;
    }

    static boolean isUsable(InetAddress address) {
        return address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress();
    }

    static String firstUsable(List<InetAddress> addresses) {
        for (InetAddress address : addresses) {
            if (isUsable(address)) {
                return address.getHostAddress();
            }
        }
        return null;
    }
}
