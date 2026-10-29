package com.nison.freedeck.system;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.nison.freedeck.R;

import java.util.ArrayList;
import java.util.List;

/** Opens the screens of Fire TV's own Settings. */
public final class SystemSettings {

    private static final String FIRE_TV_SETTINGS_PACKAGE = "com.amazon.tv.settings.v2";

    private static final String FIRE_TV_HOME_PACKAGE = "com.amazon.tv.launcher";
    private static final String MAIN_SETTINGS_ACTION = "amazon.intent.action.MAINSETTINGS";
    private static final String MAIN_SETTINGS_CATEGORY = "android.intent.category.MAINSETTINGS";

    private SystemSettings() {
    }

    /**
     * Fire TV's full Settings menu. It is a screen of Amazon's home app, reached only through its own action and
     * category; the standard settings action is answered by that home app and just returns to Home. My Fire TV is
     * the fallback if the full menu is refused.
     */
    public static void openMain(Context context) {
        Intent full = new Intent(MAIN_SETTINGS_ACTION)
                .addCategory(MAIN_SETTINGS_CATEGORY)
                .setComponent(new ComponentName(FIRE_TV_HOME_PACKAGE, FIRE_TV_HOME_PACKAGE + ".ui.MainSettingsActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!start(context, full)) {
            open(context, FireTvSetting.MAIN);
        }
    }

    public static void openWifi(Context context) {
        open(context, FireTvSetting.NETWORK);
    }

    /**
     * Tries the screen directly, then the standard Android action for it, then Fire TV's main Settings.
     * Fire OS locks some screens to system apps, so the direct route can be refused; if nothing works the user
     * is told rather than left looking at a button that did nothing.
     */
    public static void open(Context context, FireTvSetting setting) {
        List<FirstSuccess.Attempt> attempts = new ArrayList<>();
        attempts.add(() -> start(context, new Intent().setComponent(
                new ComponentName(FIRE_TV_SETTINGS_PACKAGE, FIRE_TV_SETTINGS_PACKAGE + setting.activity))));
        if (setting.action != null) {
            attempts.add(() -> start(context, new Intent(setting.action)));
        }
        if (!FirstSuccess.of(attempts)) {
            Toast.makeText(context, R.string.settings_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean start(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
