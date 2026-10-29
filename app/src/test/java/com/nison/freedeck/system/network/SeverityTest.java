package com.nison.freedeck.system.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SeverityTest {

    @Test
    public void strongSignalsAreGreen() {
        assertEquals(Severity.GOOD, SignalQuality.fromDbm(-40).severity);
        assertEquals(Severity.GOOD, SignalQuality.fromDbm(-65).severity);
    }

    @Test
    public void fairSignalIsOrange() {
        assertEquals(Severity.WARNING, SignalQuality.fromDbm(-66).severity);
        assertEquals(Severity.WARNING, SignalQuality.fromDbm(-74).severity);
    }

    @Test
    public void weakSignalIsRed() {
        assertEquals(Severity.CRITICAL, SignalQuality.fromDbm(-75).severity);
        assertEquals(Severity.CRITICAL, SignalQuality.fromDbm(-90).severity);
    }
}
