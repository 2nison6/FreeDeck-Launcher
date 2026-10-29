package com.nison.freedeck.ui.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.AppCatalog;
import com.nison.freedeck.data.settings.AppBackground;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.BackgroundRandom;
import com.nison.freedeck.data.settings.CustomBackground;
import com.nison.freedeck.startup.HomeData;
import com.nison.freedeck.system.network.NetworkState;
import com.nison.freedeck.system.network.NetworkStatusMonitor;
import com.nison.freedeck.ui.common.BackgroundDrawables;
import com.nison.freedeck.ui.home.navbar.NavBarView;
import com.nison.freedeck.ui.home.pages.AppsPage;
import com.nison.freedeck.ui.home.pages.HomePage;
import com.nison.freedeck.ui.home.pages.SettingsPage;
import com.nison.freedeck.ui.sound.FocusSound;
import com.nison.freedeck.ui.home.pages.WifiPage;

import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.Map;

/** The launcher's main screen: a nav bar over the page it selects. Also feeds network updates to both. */
public class HomeView extends FrameLayout {

    private final NavBarView navBar;
    private final WifiPage wifiPage;
    private final NetworkStatusMonitor networkMonitor;
    private final AppCatalog appCatalog;
    private final FocusSound focusSound;
    private final AppSettings appSettings;
    private final ImageView backgroundImage;
    /** How often a random launcher background moves on. */
    private static final long ROTATE_MS = 20_000L;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private AppBackground randomShown;
    private final Runnable rotateBackground = this::rotateBackgroundNow;
    private Page selectedPage = Page.DEFAULT;
    private final Map<Page, View> pages = new EnumMap<>(Page.class);

    public HomeView(Context context) {
        this(context, null);
    }

    public HomeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_home, this, true);
        navBar = findViewById(R.id.navbar);
        wifiPage = findViewById(R.id.page_wifi);
        networkMonitor = new NetworkStatusMonitor(context);
        appCatalog = new AppCatalog(context);
        appSettings = AppSettings.from(context);
        navBar.bindSettings(appSettings);
        backgroundImage = findViewById(R.id.background_image);
        appSettings.addBackgroundListener(this::applyBackground);
        appSettings.addCustomBackgroundListener(this::applyBackground);
        focusSound = new FocusSound(context, appSettings);
        ((SettingsPage) findViewById(R.id.page_settings)).bind(appSettings, focusSound);
        ((HomePage) findViewById(R.id.page_home)).bind(appCatalog, focusSound);
        ((AppsPage) findViewById(R.id.page_apps)).bind(appCatalog, appSettings, focusSound);
        pages.put(Page.HOME, findViewById(R.id.page_home));
        pages.put(Page.APPS, findViewById(R.id.page_apps));
        pages.put(Page.SETTINGS, findViewById(R.id.page_settings));
        pages.put(Page.WIFI, wifiPage);
        navBar.setOnPageSelectedListener(this::showPage);
        showPage(Page.DEFAULT);
    }

    public void bind(HomeData data) {
        navBar.bind(data);
        wifiPage.bindPlace(data != null ? data.place : null);
    }

    /** Re-reads the network, e.g. after the location permission (needed for the Wi-Fi name) was granted. */
    public void refreshNetwork() {
        networkMonitor.refresh();
    }

    /** Returns to the home page with focus on the Home button. */
    public void resetToHome() {
        navBar.focusHome();
    }

    /** Returns to the Settings page with focus on the theme row, where the user just was. */
    public void resetToSettings() {
        navBar.focusSettings();
        post(() -> ((SettingsPage) findViewById(R.id.page_settings)).focusThemeRow());
    }

    /** Shows the Settings switch as it is now, e.g. after it was changed from a popup. */
    public void refreshDefaultLauncherSwitch() {
        ((SettingsPage) findViewById(R.id.page_settings)).refreshDefaultLauncherSwitch();
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyBackground();
        appCatalog.start();
        networkMonitor.start(this::onNetworkStateChanged);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(rotateBackground);
        networkMonitor.stop();
        appCatalog.stop();
        focusSound.release();
        super.onDetachedFromWindow();
    }

    private void onNetworkStateChanged(NetworkState state) {
        navBar.bindNetwork(state);
        wifiPage.bind(state);
    }

    /** The picture shows behind Home and All Apps only, and only when one is chosen. */
    private void applyBackground() {
        AppBackground background = appSettings.background();
        BackgroundRandom mode = appSettings.backgroundRandom();
        handler.removeCallbacks(rotateBackground);
        if (mode != BackgroundRandom.OFF) {
            List<AppBackground> pool = BackgroundDrawables.pool(getContext(), mode);
            if (!pool.contains(randomShown)) {
                randomShown = pool.isEmpty() ? null : pool.get(new Random().nextInt(pool.size()));
            }
            background = randomShown != null ? randomShown : AppBackground.NONE;
            if (pool.size() > 1 && isAttachedToWindow()) {
                handler.postDelayed(rotateBackground, ROTATE_MS);
            }
        } else {
            randomShown = null;
        }
        android.graphics.drawable.Drawable drawable = BackgroundDrawables.create(getContext(), background);
        boolean visible = drawable != null && (selectedPage == Page.HOME || selectedPage == Page.APPS);
        if (drawable != null) {
            backgroundImage.setImageDrawable(drawable);
        }
        backgroundImage.setVisibility(visible ? VISIBLE : GONE);
    }

    /** Random is on: move on to a different background from the pool, fading it in. */
    private void rotateBackgroundNow() {
        List<AppBackground> pool = BackgroundDrawables.pool(getContext(), appSettings.backgroundRandom());
        if (pool.size() < 2) {
            return;
        }
        AppBackground next;
        do {
            next = pool.get(new Random().nextInt(pool.size()));
        } while (next == randomShown);
        randomShown = next;
        backgroundImage.setAlpha(0f);
        applyBackground();
        backgroundImage.animate().alpha(1f).setDuration(800L).start();
    }

    private void showPage(Page selected) {
        selectedPage = selected;
        applyBackground();
        // Only the map needs the darkening behind the bar; elsewhere the bar is clear so the apps glow shows through.
        navBar.setBackgroundResource(selected == Page.WIFI ? R.drawable.navbar_gradient : 0);
        for (Map.Entry<Page, View> entry : pages.entrySet()) {
            entry.getValue().setVisibility(entry.getKey() == selected ? VISIBLE : GONE);
        }
    }
}
