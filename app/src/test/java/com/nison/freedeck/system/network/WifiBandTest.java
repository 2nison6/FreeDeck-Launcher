package com.nison.freedeck.system.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WifiBandTest {

    @Test
    public void twoPointFourGigahertz() {
        assertEquals(WifiBand.GHZ_2_4, WifiBand.fromMhz(2437));
    }

    @Test
    public void fiveGigahertz() {
        assertEquals(WifiBand.GHZ_5, WifiBand.fromMhz(5745));
    }

    @Test
    public void sixGigahertz() {
        assertEquals(WifiBand.GHZ_6, WifiBand.fromMhz(5955));
    }

    @Test
    public void unknownWhenNotPositive() {
        assertEquals(WifiBand.UNKNOWN, WifiBand.fromMhz(0));
        assertEquals(WifiBand.UNKNOWN, WifiBand.fromMhz(-1));
    }
}
