package com.nison.freedeck.data.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;

public class LocalAddressTest {

    private static InetAddress ip(String text) throws UnknownHostException {
        return InetAddress.getByName(text);
    }

    @Test
    public void aHomeNetworkAddressIsUsable() throws UnknownHostException {
        assertTrue(LocalAddress.isUsable(ip("192.168.0.103")));
    }

    @Test
    public void loopbackLinkLocalAndIpv6AreNot() throws UnknownHostException {
        assertFalse(LocalAddress.isUsable(ip("127.0.0.1")));
        assertFalse(LocalAddress.isUsable(ip("169.254.10.2")));
        assertFalse(LocalAddress.isUsable(ip("fe80::1")));
    }

    @Test
    public void picksTheFirstUsableOne() throws UnknownHostException {
        assertEquals("192.168.0.6", LocalAddress.firstUsable(
                Arrays.asList(ip("127.0.0.1"), ip("fe80::1"), ip("192.168.0.6"), ip("10.0.0.2"))));
        assertNull(LocalAddress.firstUsable(Collections.singletonList(ip("127.0.0.1"))));
    }
}
