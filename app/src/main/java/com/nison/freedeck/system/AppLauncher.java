package com.nison.freedeck.system;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.widget.Toast;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.InstalledApp;

/** Starts other apps. */
public final class AppLauncher {

    private AppLauncher() {
    }

    public static void launch(Context context, InstalledApp app) {
        try {
            context.startActivity(app.launchIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(context, context.getString(R.string.app_launch_failed, app.label), Toast.LENGTH_SHORT).show();
        }
    }
}
