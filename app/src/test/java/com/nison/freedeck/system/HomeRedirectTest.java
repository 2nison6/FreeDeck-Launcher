package com.nison.freedeck.system;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HomeRedirectTest {

    @Test
    public void onlyFireTvsHomeScreenCounts() {
        assertTrue(HomeRedirect.isAmazonHome("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.HomeActivity_vNext"));
        assertFalse(HomeRedirect.isAmazonHome("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity"));
        assertFalse(HomeRedirect.isAmazonHome("com.netflix.ninja", "com.netflix.ninja.HomeActivity"));
        assertFalse(HomeRedirect.isAmazonHome(null, "x.HomeActivity"));
        assertFalse(HomeRedirect.isAmazonHome("com.amazon.tv.launcher", null));
    }

    @Test
    public void nothingIsTakenOverWhileTheSwitchIsOff() {
        assertFalse(HomeRedirect.shouldTakeOver(false, "com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.HomeActivity_vNext"));
        assertTrue(HomeRedirect.shouldTakeOver(true, "com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.HomeActivity_vNext"));
    }
}
