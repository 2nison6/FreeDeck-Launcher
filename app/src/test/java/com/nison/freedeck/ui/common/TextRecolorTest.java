package com.nison.freedeck.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TextRecolorTest {

    private static final int PRIMARY = 0xFFFFFFFF;
    private static final int SECONDARY = 0xB3FFFFFF;
    private static final int CUSTOM = 0xFF6DE3F2;

    @Test
    public void theMainTextColourBecomesTheChosenOne() {
        assertEquals(CUSTOM, TextRecolor.remap(PRIMARY, PRIMARY, SECONDARY, CUSTOM));
    }

    @Test
    public void theDimmerColourKeepsItsTransparency() {
        assertEquals(0xB36DE3F2, TextRecolor.remap(SECONDARY, PRIMARY, SECONDARY, CUSTOM));
    }

    @Test
    public void otherColoursAreLeftAlone() {
        assertEquals(0xFF123456, TextRecolor.remap(0xFF123456, PRIMARY, SECONDARY, CUSTOM));
    }
}
