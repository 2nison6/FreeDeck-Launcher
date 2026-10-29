package com.nison.freedeck.system;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;

import com.nison.freedeck.R;

/**
 * The TV's own screensaver (Fire TV's, in this case). The launcher has one of its own, so the two are kept as
 * opposites: while the launcher's is on, the system's is off, and the other way round. Changing it needs the
 * WRITE_SECURE_SETTINGS permission, the same one the Home-button switch uses.
 */
public final class SystemScreensaver {

    private static final String ENABLED = "screensaver_enabled";
    private static final String ON_SLEEP = "screensaver_activate_on_sleep";
    private static final String ON_DOCK = "screensaver_activate_on_dock";

    private SystemScreensaver() {
    }

    public static boolean canChange(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Turns the system screensaver on or off. @return false when the TV does not allow this app to change it */
    public static boolean setEnabled(Context context, boolean on) {
        if (!canChange(context)) {
            return false;
        }
        ContentResolver resolver = context.getContentResolver();
        int value = on ? 1 : 0;
        try {
            Settings.Secure.putInt(resolver, ENABLED, value);
            Settings.Secure.putInt(resolver, ON_SLEEP, value);
            Settings.Secure.putInt(resolver, ON_DOCK, value);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Makes the system screensaver the opposite of the launcher's. If the TV does not allow it, says so rather than
     * leaving two screensavers that may both start.
     */
    public static void followLauncher(Context context, boolean launcherScreensaverOn) {
        if (!setEnabled(context, !launcherScreensaverOn)) {
            Toast.makeText(context, R.string.system_screensaver_unchanged, Toast.LENGTH_LONG).show();
        }
    }
}
