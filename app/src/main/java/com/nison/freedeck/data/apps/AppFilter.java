package com.nison.freedeck.data.apps;

import android.content.pm.ApplicationInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Decides which installed apps belong in the app grid. */
public final class AppFilter {

    /**
     * System apps that people use like any other app and so are shown anyway: Prime Video, the Internet (Silk)
     * browser and Amazon Music. Fire OS ships them as system apps, which would otherwise hide them.
     * Other bundled apps (Amazon Luna, troubleshooting) stay hidden.
     */
    private static final Set<String> SYSTEM_APPS_TO_SHOW = new HashSet<>(Arrays.asList(
            "com.amazon.firebat",
            "com.amazon.cloud9",
            "com.amazon.bueller.music"));

    private AppFilter() {
    }

    /** User-installed apps, plus the few system apps in {@link #SYSTEM_APPS_TO_SHOW}. Never the launcher itself. */
    public static boolean shouldShow(int applicationFlags, String packageName, String ownPackageName) {
        if (packageName.equals(ownPackageName)) {
            return false;
        }
        boolean isSystem = (applicationFlags & ApplicationInfo.FLAG_SYSTEM) != 0;
        return !isSystem || SYSTEM_APPS_TO_SHOW.contains(packageName);
    }
}
