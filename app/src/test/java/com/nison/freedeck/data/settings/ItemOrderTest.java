package com.nison.freedeck.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemOrderTest {

    private static final String[] DEFAULTS = {"a", "b", "c", "d"};

    @Test
    public void nothingSavedMeansTheDefaultOrder() {
        assertEquals(Arrays.asList(DEFAULTS), ItemOrder.parse(null, DEFAULTS, s -> s));
    }

    @Test
    public void aSavedOrderIsRestored() {
        assertEquals(Arrays.asList("c", "a", "d", "b"), ItemOrder.parse("c,a,d,b", DEFAULTS, s -> s));
    }

    @Test
    public void unknownRepeatedAndMissingItemsAreRepaired() {
        assertEquals(Arrays.asList("c", "a", "b", "d"), ItemOrder.parse("c,zzz,a,c", DEFAULTS, s -> s));
    }

    @Test
    public void formatAndParseRoundTrip() {
        List<String> order = Arrays.asList("d", "c", "b", "a");
        assertEquals(order, ItemOrder.parse(ItemOrder.format(order, s -> s), DEFAULTS, s -> s));
    }

    @Test
    public void swapExchangesTwoItemsAndRejectsBadPositions() {
        List<String> items = new ArrayList<>(Arrays.asList(DEFAULTS));
        assertTrue(ItemOrder.swap(items, 0, 2));
        assertEquals(Arrays.asList("c", "b", "a", "d"), items);
        assertFalse(ItemOrder.swap(items, 3, 4));
        assertFalse(ItemOrder.swap(items, -1, 0));
        assertEquals(Arrays.asList("c", "b", "a", "d"), items);
    }
}
