package com.nison.freedeck.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FirstSuccessTest {

    private static FirstSuccess.Attempt attempt(List<String> log, String name, boolean works) {
        return () -> {
            log.add(name);
            return works;
        };
    }

    @Test
    public void stopsAtTheFirstThatWorks() {
        List<String> log = new ArrayList<>();
        boolean opened = FirstSuccess.of(Arrays.asList(
                attempt(log, "direct", true), attempt(log, "action", true), attempt(log, "main", true)));
        assertTrue(opened);
        assertEquals(Collections.singletonList("direct"), log);
    }

    @Test
    public void fallsThroughToTheNextWhenOneIsRefused() {
        List<String> log = new ArrayList<>();
        boolean opened = FirstSuccess.of(Arrays.asList(
                attempt(log, "direct", false), attempt(log, "action", true), attempt(log, "main", true)));
        assertTrue(opened);
        assertEquals(Arrays.asList("direct", "action"), log);
    }

    @Test
    public void triesEveryWayBeforeGivingUp() {
        List<String> log = new ArrayList<>();
        boolean opened = FirstSuccess.of(Arrays.asList(
                attempt(log, "direct", false), attempt(log, "action", false), attempt(log, "main", false)));
        assertFalse(opened);
        assertEquals(Arrays.asList("direct", "action", "main"), log);
    }

    @Test
    public void noAttemptsMeansNothingWorked() {
        assertFalse(FirstSuccess.of(Collections.<FirstSuccess.Attempt>emptyList()));
    }
}
