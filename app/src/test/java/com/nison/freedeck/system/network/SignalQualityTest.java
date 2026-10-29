package com.nison.freedeck.system.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SignalQualityTest {

    @Test
    public void strongSignalIsExcellent() {
        assertEquals(SignalQuality.EXCELLENT, SignalQuality.fromDbm(-40));
        assertEquals(SignalQuality.EXCELLENT, SignalQuality.fromDbm(-55));
    }

    @Test
    public void goodRange() {
        assertEquals(SignalQuality.GOOD, SignalQuality.fromDbm(-56));
        assertEquals(SignalQuality.GOOD, SignalQuality.fromDbm(-65));
    }

    @Test
    public void fairRange() {
        assertEquals(SignalQuality.FAIR, SignalQuality.fromDbm(-66));
        assertEquals(SignalQuality.FAIR, SignalQuality.fromDbm(-74));
    }

    @Test
    public void weakFromMinus75Down() {
        assertEquals(SignalQuality.WEAK, SignalQuality.fromDbm(-75));
        assertEquals(SignalQuality.WEAK, SignalQuality.fromDbm(-90));
    }
}
