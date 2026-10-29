package com.nison.freedeck.ui.main;

import android.app.Activity;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.startup.HomeData;
import com.nison.freedeck.system.DefaultLauncher;
import com.nison.freedeck.startup.LauncherStartup;
import com.nison.freedeck.ui.home.HomeView;
import com.nison.freedeck.ui.loader.LoaderView;
import com.nison.freedeck.ui.screensaver.ScreensaverView;

/** Launcher entry point: loops the loader animation while startup runs, then reveals the home screen. */
public class MainActivity extends Activity {

    private static final int REQUEST_LOCATION = 1;

    /** Longest the loader is kept if the animation never reports it finished (say, the video cannot play). */
    private static final long LOADER_GIVE_UP_MS = 15_000L;

    private LauncherStartup startup;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private HomeData loadedData;
    private boolean dataReady;
    private boolean animationDone;
    private boolean revealed;

    private LoaderView loader;
    private HomeView home;
    private ScreensaverView screensaver;
    private AppSettings settings;
    private final Runnable startScreensaver = this::showScreensaver;

    /** Looks every resource up for the chosen colour mode (dark or light) and language, whatever the TV itself is set to. */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.nison.freedeck.ui.common.AppContext.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.nison.freedeck.ui.common.TextStyler.install(getLayoutInflater(), this, AppSettings.from(this));
        setContentView(R.layout.activity_main);
        loader = findViewById(R.id.loader);
        home = findViewById(R.id.home);
        screensaver = findViewById(R.id.screensaver);
        settings = AppSettings.from(this);
        settings.addScreensaverListener(this::restartIdleTimer);

        startup = new LauncherStartup(this);
        HomeData kept = LauncherSession.takeIfRestarting();
        if (kept != null) {
            // Rebuilt for a colour mode change: straight back to Settings, with no loader.
            home.bind(kept);
            loader.stop();
            home.show();
            home.resetToSettings();
            revealed = true;
            return;
        }

        home.hide();
        loader.setOnFirstLoopCompleted(() -> {
            animationDone = true;
            revealWhenReady();
        });
        loader.start();
        handler.postDelayed(() -> {
            animationDone = true;
            revealWhenReady();
        }, LOADER_GIVE_UP_MS);
        startup.start(data -> {
            loadedData = data;
            dataReady = true;
            revealWhenReady();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restartIdleTimer();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(startScreensaver);
        screensaver.stop();
        super.onPause();
    }

    /** Any key press ends the screensaver (and is not passed on, so waking it never clicks something). */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (screensaver.isShowing()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                screensaver.stop();
                restartIdleTimer();
            }
            return true;
        }
        restartIdleTimer();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (screensaver.isShowing()) {
            screensaver.stop();
            restartIdleTimer();
            return true;
        }
        restartIdleTimer();
        return super.dispatchGenericMotionEvent(event);
    }

    /** Counts the idle time from now; does nothing while the screensaver is off or the home screen is not up yet. */
    private void restartIdleTimer() {
        handler.removeCallbacks(startScreensaver);
        // While ours is enabled the window keeps the screen awake, so Fire TV's own screensaver never gets its turn.
        if (settings.isScreensaverOn()) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (revealed && settings.isScreensaverOn() && !screensaver.isShowing()) {
            handler.postDelayed(startScreensaver, settings.screensaverDelay().millis());
        }
    }

    private void showScreensaver() {
        if (revealed && settings.isScreensaverOn()) {
            screensaver.start(settings, loadedData);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // The Home button was pressed while the launcher is already running.
        super.onNewIntent(intent);
        home.resetToHome();
    }

    @Override
    protected void onStop() {
        // Returning to the launcher later (Home button) should land on the home screen, not replay the loader.
        showHome();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        startup.cancel();
        super.onDestroy();
    }

    /** The home screen replaces the loader only when the data is in and the animation has played right through. */
    private void revealWhenReady() {
        if (revealed || !dataReady || !animationDone) {
            return;
        }
        revealed = true;
        onStartupFinished(loadedData);
        restartIdleTimer();
    }

    private void onStartupFinished(HomeData data) {
        LauncherSession.remember(data);
        home.bind(data);
        showHome();
        home.resetToHome();
        requestWifiNamePermission();
        offerDefaultLauncher();
        if (data.place == null || data.weather == null) {
            // Not everything could be loaded (the network was probably still coming up): keep trying quietly.
            startup.refreshLater(data, later -> {
                LauncherSession.remember(later);
                home.bind(later);
                loadedData = later;
            });
        }
    }

    /** When the launcher opens with "Make Default Launcher" off (including the very first time), offer to turn it on. */
    private void offerDefaultLauncher() {
        AppSettings settings = AppSettings.from(this);
        if (settings.isDefaultLauncher()) {
            // Already allowed before. Reinstalling the app switches its Home-button service off in the system, so
            // turn it back on quietly when permitted; never ask again, and never open the accessibility screen here.
            if (DefaultLauncher.enableQuietly(this)) {
                home.refreshDefaultLauncherSwitch();
            }
            return;
        }
        if (settings.isDefaultLauncherPromptDenied() || isFinishing()) {
            return;
        }
        new DefaultLauncherPrompt(this, home::refreshDefaultLauncherSwitch).show();
    }

    /** Android only reveals the Wi-Fi network name to apps that hold the location permission. */
    private void requestWifiNamePermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            home.refreshNetwork();
        }
    }

    private void showHome() {
        loader.stop();
        home.show();
    }
}
