package com.nison.freedeck.system.adb;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;

/**
 * Lets the app give itself the one permission Fire TV will not hand out through a popup, by asking the TV's own
 * debugging service to grant it. This needs "ADB debugging" switched on in Developer options; the TV then shows its
 * standard "Allow USB debugging?" question, and nothing else is needed from the user.
 */
public final class AdbGrant {

    public enum Result {
        GRANTED,
        /** ADB debugging is off (or the TV refuses local connections). */
        DEBUGGING_OFF,
        /** The "Allow USB debugging?" question went unanswered or was refused. */
        NOT_ALLOWED,
        FAILED
    }

    public interface Callback {
        void onResult(Result result);
    }

    private static final String PREFS = "adb_grant";
    private static final String KEY_PRIVATE = "private_key";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5555;
    private static final String PERMISSION = "android.permission.WRITE_SECURE_SETTINGS";

    private AdbGrant() {
    }

    /** Grants {@code WRITE_SECURE_SETTINGS} to this app in the background; {@code callback} runs on the main thread. */
    public static void grantSecureSettings(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            Result result = run(app);
            main.post(() -> callback.onResult(result));
        }, "adb-grant").start();
    }

    private static Result run(Context context) {
        try {
            AdbKey key = loadOrCreateKey(context);
            String output = AdbClient.shell(key, HOST, PORT, "pm grant " + context.getPackageName() + " " + PERMISSION);
            // A successful grant prints nothing; anything else is the shell explaining what went wrong.
            return output.isEmpty() ? Result.GRANTED : Result.FAILED;
        } catch (ConnectException e) {
            return Result.DEBUGGING_OFF;
        } catch (SocketTimeoutException e) {
            return Result.NOT_ALLOWED;
        } catch (IOException e) {
            // The TV closes the connection when the user picks "Deny".
            return Result.NOT_ALLOWED;
        } catch (GeneralSecurityException | RuntimeException e) {
            return Result.FAILED;
        }
    }

    /** The key is made once and kept, so the TV's "always allow" answer keeps working and it never asks again. */
    private static AdbKey loadOrCreateKey(Context context) throws GeneralSecurityException {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_PRIVATE, null);
        if (saved != null) {
            try {
                return AdbKey.fromEncoded(Base64.decode(saved, Base64.NO_WRAP));
            } catch (GeneralSecurityException | IllegalArgumentException e) {
                // Unreadable: fall through and make a new one.
            }
        }
        AdbKey key = AdbKey.generate();
        prefs.edit().putString(KEY_PRIVATE, Base64.encodeToString(key.encoded(), Base64.NO_WRAP)).apply();
        return key;
    }
}
