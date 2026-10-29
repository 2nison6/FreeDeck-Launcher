package com.nison.freedeck.data.apps;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/** An app that can be opened from the launcher, with what's needed to show and start it. */
public final class InstalledApp {

    public final String packageName;
    public final String label;
    /** The TV banner when the app has one, otherwise its icon. */
    public final Drawable cardImage;
    public final boolean hasBanner;
    /** Vivid colour taken from the app's icon, used for the glow behind the grid. */
    public final int accentColor;
    public final Intent launchIntent;
    /** The app's own description of itself, when it declares one; most apps don't. */
    @Nullable
    public final String description;
    /** The app's declared category, such as "Video" or "Music"; null when it doesn't declare one. */
    @Nullable
    public final String category;
    @Nullable
    public final String versionName;
    public final long installTimeMillis;

    public InstalledApp(String packageName, String label, Drawable cardImage, boolean hasBanner,
                        int accentColor, Intent launchIntent, @Nullable String description,
                        @Nullable String category, @Nullable String versionName, long installTimeMillis) {
        this.packageName = packageName;
        this.label = label;
        this.cardImage = cardImage;
        this.hasBanner = hasBanner;
        this.accentColor = accentColor;
        this.launchIntent = launchIntent;
        this.description = description;
        this.category = category;
        this.versionName = versionName;
        this.installTimeMillis = installTimeMillis;
    }
}
