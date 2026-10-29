package com.nison.freedeck.system;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.nison.freedeck.system.adb.AdbGrant;

/**
 * Turning the "make default launcher" switch on: makes sure the Home-button service is allowed to run. Fire TV has no
 * screen to switch an accessibility service on from, so the app switches it on itself, which needs the
 * WRITE_SECURE_SETTINGS permission. That permission cannot be asked for with a popup; the app grants it to itself
 * through the TV's own debugging service, or the user grants it once with adb.
 */
public final class DefaultLauncher {

    /** What happened when the service was asked to turn on. */
    public enum Outcome {
        ENABLED,
        /** ADB debugging is off in Developer options, so the app cannot grant itself the permission. */
        DEBUGGING_OFF,
        /** The "Allow USB debugging?" question was not answered or was refused. */
        NOT_ALLOWED,
        FAILED
    }

    public interface Callback {
        void onOutcome(Outcome outcome);
    }

    private DefaultLauncher() {
    }

    private static ComponentName service(Context context) {
        return new ComponentName(context, HomeRedirectService.class);
    }

    /** The command that gives the app the permission, for showing to the user. */
    public static String grantCommand(Context context) {
        return "adb shell pm grant " + context.getPackageName() + " android.permission.WRITE_SECURE_SETTINGS";
    }

    public static boolean isServiceEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return HomeRedirect.isListed(enabled, service(context));
    }

    public static boolean canEnableByItself(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Turns the service on when the app already holds the permission. @return true when the service is on afterwards */
    public static boolean enableQuietly(Context context) {
        if (isServiceEnabled(context)) {
            return true;
        }
        if (!canEnableByItself(context)) {
            return false;
        }
        try {
            String current = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    HomeRedirect.withService(current, service(context)));
            Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Gets the service running: straight away when the permission is already held, otherwise by granting it through
     * the TV's debugging service first. {@code callback} runs on the main thread with the result.
     */
    public static void enable(Context context, Callback callback) {
        if (enableQuietly(context)) {
            callback.onOutcome(Outcome.ENABLED);
            return;
        }
        AdbGrant.grantSecureSettings(context, result -> {
            if (result == AdbGrant.Result.GRANTED && enableQuietly(context)) {
                callback.onOutcome(Outcome.ENABLED);
                return;
            }
            switch (result) {
                case DEBUGGING_OFF:
                    callback.onOutcome(Outcome.DEBUGGING_OFF);
                    break;
                case NOT_ALLOWED:
                    callback.onOutcome(Outcome.NOT_ALLOWED);
                    break;
                default:
                    callback.onOutcome(Outcome.FAILED);
                    break;
            }
        });
    }
}
