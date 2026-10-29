package com.nison.freedeck.data.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AppDescriptionTest {

    @Test
    public void nullAndBlankHaveNoText() {
        assertNull(AppDescription.clean(null));
        assertNull(AppDescription.clean(""));
        assertNull(AppDescription.clean("   \n\t "));
    }

    @Test
    public void whitespaceIsCollapsedAndTrimmed() {
        assertEquals("Watch movies and shows", AppDescription.clean("  Watch\n movies   and\tshows \n"));
    }

    @Test
    public void ordinaryTextIsUnchanged() {
        assertEquals("Stream music", AppDescription.clean("Stream music"));
    }

    @Test
    public void joinSkipsMissingParts() {
        assertEquals("Video · Version 9.1", AppDescription.join(" · ", "Video", "Version 9.1"));
        assertEquals("Version 9.1", AppDescription.join(" · ", null, "Version 9.1"));
        assertEquals("Video", AppDescription.join(" · ", "Video", null));
        assertEquals("Video", AppDescription.join(" · ", "Video", "  "));
    }

    @Test
    public void joinOfNothingIsEmpty() {
        assertEquals("", AppDescription.join(" · "));
        assertEquals("", AppDescription.join(" · ", null, ""));
    }
}
