package com.nison.freedeck.data.apps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.ApplicationInfo;

import org.junit.Test;

public class AppFilterTest {

    private static final String OWN = "com.nison.freedeck";
    private static final int SYSTEM = ApplicationInfo.FLAG_SYSTEM;
    private static final int UPDATED_SYSTEM = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

    @Test
    public void userInstalledAppIsShown() {
        assertTrue(AppFilter.shouldShow(0, "com.netflix.ninja", OWN));
    }

    @Test
    public void systemAppIsHidden() {
        assertFalse(AppFilter.shouldShow(SYSTEM, "com.amazon.firetv.troubleshooting", OWN));
    }

    @Test
    public void updatedSystemAppIsHidden() {
        assertFalse(AppFilter.shouldShow(UPDATED_SYSTEM, "com.amazon.spiderpork", OWN)); // Amazon Luna
    }

    @Test
    public void primeVideoIsShownEvenThoughItIsASystemApp() {
        assertTrue(AppFilter.shouldShow(UPDATED_SYSTEM, "com.amazon.firebat", OWN));
        assertTrue(AppFilter.shouldShow(SYSTEM, "com.amazon.firebat", OWN));
    }

    @Test
    public void internetBrowserAndAmazonMusicAreShownToo() {
        assertTrue(AppFilter.shouldShow(UPDATED_SYSTEM, "com.amazon.cloud9", OWN)); // Internet (Silk)
        assertTrue(AppFilter.shouldShow(UPDATED_SYSTEM, "com.amazon.bueller.music", OWN)); // Amazon Music
        assertFalse(AppFilter.shouldShow(SYSTEM, "com.amazon.venezia", OWN)); // the Appstore stays out
    }

    @Test
    public void theLauncherItselfIsHidden() {
        assertFalse(AppFilter.shouldShow(0, OWN, OWN));
    }

    @Test
    public void unrelatedFlagsDoNotHideAnApp() {
        assertTrue(AppFilter.shouldShow(ApplicationInfo.FLAG_HAS_CODE | ApplicationInfo.FLAG_ALLOW_BACKUP, "com.spotify.tv.android", OWN));
    }

    @Test
    public void aNameThatOnlyContainsAnAllowedPackageIsStillHidden() {
        assertFalse(AppFilter.shouldShow(SYSTEM, "com.amazon.firebat.helper", OWN));
    }
}
