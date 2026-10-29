package com.nison.freedeck.ui.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GridSizingTest {

    // A card 300 wide is 200 tall (a 16:9 picture plus 40 for the name); at 150 wide it is 130 tall.
    private static final int BASE = 300;
    private static final int AT_BASE = 217;
    private static final int AT_HALF = 135;

    @Test
    public void cardsKeepTheirFullWidthWhenTheRowsFit() {
        assertEquals(BASE, GridSizing.itemWidth(BASE, AT_BASE, AT_HALF, 300));
    }

    @Test
    public void cardsShrinkSoTheRowsFitTheHeight() {
        int width = GridSizing.itemWidth(BASE, AT_BASE, AT_HALF, 180);
        assertTrue(width < BASE);
        assertTrue(GridSizing.heightAt(width, BASE, AT_BASE, AT_HALF) <= 180);
        assertTrue(GridSizing.heightAt(width + 2, BASE, AT_BASE, AT_HALF) > 180);
    }

    @Test
    public void heightAtTheMeasuredWidthsIsWhatWasMeasured() {
        assertEquals(AT_BASE, GridSizing.heightAt(BASE, BASE, AT_BASE, AT_HALF));
        assertEquals(AT_HALF, GridSizing.heightAt(BASE / 2, BASE, AT_BASE, AT_HALF));
    }
}
