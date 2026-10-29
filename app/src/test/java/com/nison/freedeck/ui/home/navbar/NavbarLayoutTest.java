package com.nison.freedeck.ui.home.navbar;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavbarLayoutTest {

    @Test
    public void everythingShownHasADividerBetweenEach() {
        assertArrayEquals(new boolean[]{true, true}, NavbarLayout.dividersBefore(true, true, true));
    }

    @Test
    public void hidingTheMiddleLeavesOneDivider() {
        assertArrayEquals(new boolean[]{false, true}, NavbarLayout.dividersBefore(true, false, true));
    }

    @Test
    public void hidingTheFirstDropsTheDividerBeforeTheNextOne() {
        assertArrayEquals(new boolean[]{false, true}, NavbarLayout.dividersBefore(false, true, true));
    }

    @Test
    public void hidingTheLastLeavesNoTrailingDivider() {
        assertArrayEquals(new boolean[]{true, false}, NavbarLayout.dividersBefore(true, true, false));
    }

    @Test
    public void aSinglePieceNeedsNoDivider() {
        assertArrayEquals(new boolean[]{false, false}, NavbarLayout.dividersBefore(false, true, false));
        assertArrayEquals(new boolean[0], NavbarLayout.dividersBefore(true));
    }

    @Test
    public void anyShownSaysWhetherARowHasContent() {
        assertTrue(NavbarLayout.anyShown(false, true));
        assertFalse(NavbarLayout.anyShown(false, false));
    }
}
