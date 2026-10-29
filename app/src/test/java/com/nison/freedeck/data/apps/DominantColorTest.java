package com.nison.freedeck.data.apps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

public class DominantColorTest {

    private static final int FALLBACK = 0xFF123456;

    private static int[] fill(int color, int count) {
        int[] pixels = new int[count];
        Arrays.fill(pixels, color);
        return pixels;
    }

    private static int[] concat(int[] a, int[] b) {
        int[] all = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, all, a.length, b.length);
        return all;
    }

    @Test
    public void pureRed() {
        assertEquals(0xFFFF0000, DominantColor.fromPixels(fill(0xFFFF0000, 50), FALLBACK));
    }

    @Test
    public void pureGreen() {
        assertEquals(0xFF00FF00, DominantColor.fromPixels(fill(0xFF00FF00, 50), FALLBACK));
    }

    @Test
    public void pureBlue() {
        assertEquals(0xFF0000FF, DominantColor.fromPixels(fill(0xFF0000FF, 50), FALLBACK));
    }

    @Test
    public void majorityColourWins() {
        int[] pixels = concat(fill(0xFF00FF00, 70), fill(0xFFFF0000, 30));
        assertEquals(0xFF00FF00, DominantColor.fromPixels(pixels, FALLBACK));
    }

    @Test
    public void greysAndBlackAndWhiteAreIgnored() {
        int[] pixels = concat(concat(fill(0xFF808080, 40), fill(0xFFFFFFFF, 40)), fill(0xFF000000, 40));
        assertEquals(FALLBACK, DominantColor.fromPixels(pixels, FALLBACK));
    }

    @Test
    public void aColourfulMinorityBeatsABackgroundOfBlack() {
        int[] pixels = concat(fill(0xFF000000, 900), fill(0xFFE50914, 100)); // a red logo on black, like Netflix
        int result = DominantColor.fromPixels(pixels, FALLBACK);
        assertEquals(0xFF, result >>> 24);
        assertEquals(0xFF, (result >> 16) & 0xFF); // red channel is full brightness
    }

    @Test
    public void transparentPixelsAreIgnored() {
        assertEquals(FALLBACK, DominantColor.fromPixels(fill(0x00FF0000, 50), FALLBACK));
    }

    @Test
    public void emptyImageGivesTheFallback() {
        assertEquals(FALLBACK, DominantColor.fromPixels(new int[0], FALLBACK));
    }

    @Test
    public void palePastelIsTooWashedOut() {
        assertEquals(FALLBACK, DominantColor.fromPixels(fill(0xFFFFCCCC, 50), FALLBACK));
    }

    @Test
    public void saturationIsLiftedToAtLeastSixtyPercent() {
        assertEquals(0xFFFF6666, DominantColor.fromPixels(fill(0xFFFF6666, 50), FALLBACK));
    }

    @Test
    public void redsOnBothSidesOfZeroDegreesCombine() {
        int[] pixels = concat(fill(0xFFFF2040, 50), fill(0xFFFF4020, 50)); // hues about -8 and +8 degrees
        assertEquals(0xFFFF2020, DominantColor.fromPixels(pixels, FALLBACK));
    }
}
