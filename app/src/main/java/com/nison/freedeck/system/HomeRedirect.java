package com.nison.freedeck.system;

import android.content.ComponentName;

/** The rules for taking over the Home button, kept free of Android classes so they can be tested. */
public final class HomeRedirect {

    /** Fire TV's own launcher. */
    public static final String AMAZON_LAUNCHER_PACKAGE = "com.amazon.tv.launcher";

    private HomeRedirect() {
    }

    /**
     * Whether a window that just appeared is Fire TV's home screen. Only the home screen counts: the launcher's
     * other screens (its Settings, for one) must stay reachable, or "More settings" would bounce straight back.
     */
    public static boolean isAmazonHome(CharSequence packageName, CharSequence className) {
        return packageName != null && className != null
                && AMAZON_LAUNCHER_PACKAGE.contentEquals(packageName)
                && className.toString().contains("HomeActivity");
    }

    /** Whether to open our launcher: the switch is on and Fire TV's home screen has just come up. */
    public static boolean shouldTakeOver(boolean switchedOn, CharSequence packageName, CharSequence className) {
        return switchedOn && isAmazonHome(packageName, className);
    }

    /** Whether {@code service} appears in the system's colon-separated list of enabled accessibility services. */
    public static boolean isListed(String enabledServices, ComponentName service) {
        if (enabledServices == null || enabledServices.isEmpty()) {
            return false;
        }
        String full = service.flattenToString();
        String shortForm = service.flattenToShortString();
        for (String entry : enabledServices.split(":")) {
            if (entry.equalsIgnoreCase(full) || entry.equalsIgnoreCase(shortForm)) {
                return true;
            }
        }
        return false;
    }

    /** The list with {@code service} added (once). */
    public static String withService(String enabledServices, ComponentName service) {
        if (isListed(enabledServices, service)) {
            return enabledServices;
        }
        String full = service.flattenToString();
        return enabledServices == null || enabledServices.isEmpty() ? full : enabledServices + ":" + full;
    }
}
