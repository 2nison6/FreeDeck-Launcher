package com.nison.freedeck.ui.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BackgroundDrawablesTest {

    @Test
    public void withAlphaKeepsTheColourAndSetsTheAlpha() {
        assertEquals(0x80FF6A3D, BackgroundDrawables.withAlpha(0xFFFF6A3D, 0x80));
        assertEquals(0x00FF6A3D, BackgroundDrawables.withAlpha(0xFFFF6A3D, 0));
    }

    @Test
    public void overAnOpaqueTopIsTheTop() {
        assertEquals(0xFFFF6A3D, BackgroundDrawables.over(0xFFFF6A3D, 0xFF000000));
    }

    @Test
    public void overATransparentTopIsTheBottom() {
        assertEquals(0xFFF2F2F7, BackgroundDrawables.over(0x00FF6A3D, 0xFFF2F2F7));
    }

    @Test
    public void overHalfwayIsInTheMiddle() {
        assertEquals(0xFF808080, BackgroundDrawables.over(0x80FFFFFF, 0xFF000000));
    }
}
