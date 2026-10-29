package com.nison.freedeck.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FireTvSettingTest {

    @Test
    public void screenIsRelativeToTheSettingsPackage() {
        for (FireTvSetting setting : FireTvSetting.values()) {
            assertTrue(setting.activity.startsWith("."));
        }
    }

    @Test
    public void networkOpensTheScreenTheWifiButtonUses() {
        assertEquals(".tv.network.NetworkActivity", FireTvSetting.NETWORK.activity);
        assertEquals("android.settings.WIFI_SETTINGS", FireTvSetting.NETWORK.action);
    }
}
