package com.nison.freedeck.data.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.nison.freedeck.R;

public class AppOverridesTest {

    @Test
    public void silkBecomesInternetWithItsOwnPicture() {
        AppOverrides.Override silk = AppOverrides.forPackage("com.amazon.cloud9");
        assertNotNull(silk);
        assertEquals(R.string.app_name_internet, silk.label);
        assertEquals(R.drawable.app_banner_internet, silk.banner);
    }

    @Test
    public void theAppstoreIsNotInTheLauncherAtAll() {
        assertNull(AppOverrides.forPackage("com.amazon.venezia"));
        assertFalse(AppFilter.shouldShow(android.content.pm.ApplicationInfo.FLAG_SYSTEM, "com.amazon.venezia", "com.nison.freedeck"));
    }

    @Test
    public void everyOtherAppKeepsItsOwnNameAndPicture() {
        assertNull(AppOverrides.forPackage("com.netflix.ninja"));
        assertNull(AppOverrides.forPackage("com.amazon.firebat"));
    }
}
