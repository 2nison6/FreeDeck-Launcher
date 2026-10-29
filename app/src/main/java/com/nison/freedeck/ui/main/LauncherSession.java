package com.nison.freedeck.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import com.nison.freedeck.startup.HomeData;

/**
 * What survives while the launcher's screen is rebuilt in the other colour mode: the data already loaded, so
 * neither the loader nor the startup work has to run again, and where to put the user back.
 */
public final class LauncherSession {

    private static HomeData data;
    private static boolean restartingForTheme;

    private LauncherSession() {
    }

    static void remember(HomeData loaded) {
        data = loaded;
    }

    /** The loaded data to reuse, if this rebuild is a theme switch; null otherwise (a normal start). */
    static HomeData takeIfRestarting() {
        if (!restartingForTheme) {
            return null;
        }
        restartingForTheme = false;
        return data;
    }

    /** Rebuilds the screen so every resource is looked up again for the mode or language that was just saved. */
    public static void restartForTheme(Context context) {
        Activity activity = activityOf(context);
        if (activity == null) {
            return;
        }
        restartingForTheme = data != null;
        activity.recreate();
    }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
