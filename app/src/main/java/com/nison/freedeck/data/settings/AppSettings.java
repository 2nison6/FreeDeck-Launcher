package com.nison.freedeck.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** The launcher's own settings, kept across restarts. */
public class AppSettings {

    /** Where the values are kept; lets the logic be tested without Android. */
    public interface Storage {
        String read(String key);

        void write(String key, String value);
    }

    private static final String PREFERENCES_NAME = "launcher";
    private static final String KEY_NAVIGATION_SOUND = "navigation_sound";
    private static final String KEY_APP_LAYOUT = "apps_layout";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_CLOCK_FORMAT = "clock_format";
    private static final String KEY_FONT_STYLE = "font_style";
    private static final String KEY_FONT_COLOR_PREFIX = "font_color_";
    private static final String KEY_NAVBAR_TEXT_ORDER = "navbar_text_order";
    private static final String KEY_NAVBAR_BUTTON_ORDER = "navbar_button_order";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DEFAULT_LAUNCHER = "default_launcher";
    private static final String KEY_DEFAULT_LAUNCHER_DENIED = "default_launcher_denied";
    private static final String KEY_NAVBAR_SHOW_PREFIX = "navbar_show_";
    private static final String KEY_SCREENSAVER_ON = "screensaver_on";
    private static final String KEY_SCREENSAVER_BACKGROUND = "screensaver_background";
    private static final String KEY_SCREENSAVER_WIDGET_PREFIX = "screensaver_widget_";
    private static final String KEY_SCREENSAVER_MOTION = "screensaver_motion";
    private static final String KEY_SCREENSAVER_DELAY = "screensaver_delay";

    private final Storage storage;
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<Runnable> backgroundListeners = new ArrayList<>();
    private final List<Runnable> navbarListeners = new ArrayList<>();
    private final List<Runnable> screensaverListeners = new ArrayList<>();
    private final List<Runnable> customBackgroundListeners = new ArrayList<>();

    public AppSettings(Storage storage) {
        this.storage = storage;
    }

    public static AppSettings from(Context context) {
        // An activity asks while it is still being set up, when its application context may not exist yet.
        Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        SharedPreferences preferences = app.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return new AppSettings(new Storage() {
            @Override
            public String read(String key) {
                return preferences.getString(key, null);
            }

            @Override
            public void write(String key, String value) {
                preferences.edit().putString(key, value).apply();
            }
        });
    }

    public NavigationSound navigationSound() {
        return NavigationSound.fromId(storage.read(KEY_NAVIGATION_SOUND));
    }

    public void setNavigationSound(NavigationSound sound) {
        storage.write(KEY_NAVIGATION_SOUND, sound.id);
    }

    public AppLayout appLayout() {
        return AppLayout.fromId(storage.read(KEY_APP_LAYOUT));
    }

    public void setAppLayout(AppLayout layout) {
        storage.write(KEY_APP_LAYOUT, layout.id);
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    /** Called whenever the app layout is changed, so a page that is not on screen can follow it. */
    public void addAppLayoutListener(Runnable listener) {
        listeners.add(listener);
    }

    public ThemeMode themeMode() {
        return ThemeMode.fromId(storage.read(KEY_THEME_MODE));
    }

    public void setThemeMode(ThemeMode mode) {
        storage.write(KEY_THEME_MODE, mode.id);
    }

    /** Whether the Home button should open this launcher instead of Fire TV's. Off until the user turns it on. */
    public boolean isDefaultLauncher() {
        return "1".equals(storage.read(KEY_DEFAULT_LAUNCHER));
    }

    public void setDefaultLauncher(boolean on) {
        storage.write(KEY_DEFAULT_LAUNCHER, on ? "1" : "0");
    }

    /** Whether the user pressed Deny on the "make default launcher" popup; if so it is never shown again. */
    public boolean isDefaultLauncherPromptDenied() {
        return "1".equals(storage.read(KEY_DEFAULT_LAUNCHER_DENIED));
    }

    public void setDefaultLauncherPromptDenied(boolean denied) {
        storage.write(KEY_DEFAULT_LAUNCHER_DENIED, denied ? "1" : "0");
    }

    public AppLanguage language() {
        return AppLanguage.fromCode(storage.read(KEY_LANGUAGE));
    }

    public void setLanguage(AppLanguage language) {
        storage.write(KEY_LANGUAGE, language.code);
    }

    public FontStyle fontStyle() {
        return FontStyle.fromId(storage.read(KEY_FONT_STYLE));
    }

    public void setFontStyle(FontStyle style) {
        storage.write(KEY_FONT_STYLE, style.id);
    }

    /** The text colour chosen for the mode in use now, or null for the launcher's own; each mode has its own. */
    public Integer fontColor() {
        return fontColor(themeMode());
    }

    public Integer fontColor(ThemeMode mode) {
        String saved = storage.read(KEY_FONT_COLOR_PREFIX + mode.id);
        if (saved == null || !saved.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        return 0xFF000000 | Integer.parseInt(saved, 16);
    }

    /** @param color the colour for that mode, or null to go back to the launcher's own */
    public void setFontColor(ThemeMode mode, Integer color) {
        storage.write(KEY_FONT_COLOR_PREFIX + mode.id,
                color == null ? "" : String.format("%06x", color & 0xFFFFFF));
    }

    /** The launcher's own fixed background; while random is on, the one it will return to when random is turned off. */
    public AppBackground background() {
        return AppBackground.fromId(storage.read(KEY_BACKGROUND));
    }

    public void setBackground(AppBackground background) {
        storage.write(KEY_BACKGROUND, background.id);
        notifyBackgroundListeners();
    }

    /** Whether the launcher cycles its background by itself, and through which pictures. */
    public BackgroundRandom backgroundRandom() {
        return BackgroundRandom.fromId(storage.read(KEY_BACKGROUND));
    }

    public void setBackgroundRandom(BackgroundRandom mode) {
        storage.write(KEY_BACKGROUND, mode.id == null ? background().id : mode.id);
        notifyBackgroundListeners();
    }

    private void notifyBackgroundListeners() {
        for (Runnable listener : new ArrayList<>(backgroundListeners)) {
            listener.run();
        }
    }

    /** Called whenever the background is changed, so the screen behind the pages can follow it. */
    public void addBackgroundListener(Runnable listener) {
        backgroundListeners.add(listener);
    }

    public ClockFormat clockFormat() {
        return ClockFormat.fromId(storage.read(KEY_CLOCK_FORMAT));
    }

    public void setClockFormat(ClockFormat format) {
        storage.write(KEY_CLOCK_FORMAT, format.id);
        notifyNavbarListeners();
    }

    /** Whether the navbar shows this piece: what the user chose, or else its default. */
    public boolean isNavbarItemShown(NavbarItem item) {
        String saved = storage.read(KEY_NAVBAR_SHOW_PREFIX + item.id);
        return saved == null ? item.shownByDefault : "1".equals(saved);
    }

    public void setNavbarItemShown(NavbarItem item, boolean shown) {
        storage.write(KEY_NAVBAR_SHOW_PREFIX + item.id, shown ? "1" : "0");
        notifyNavbarListeners();
    }

    /** Where each piece of navbar text goes: the first three on the top row, the rest on the bottom row. */
    public List<NavbarItem> navbarTextOrder() {
        return ItemOrder.parse(storage.read(KEY_NAVBAR_TEXT_ORDER), NavbarItem.values(), item -> item.id);
    }

    public void setNavbarTextOrder(List<NavbarItem> order) {
        storage.write(KEY_NAVBAR_TEXT_ORDER, ItemOrder.format(order, item -> item.id));
        notifyNavbarListeners();
    }

    /** Left to right order of the navbar's icon buttons. */
    public List<NavButton> navButtonOrder() {
        return ItemOrder.parse(storage.read(KEY_NAVBAR_BUTTON_ORDER), NavButton.values(), button -> button.id);
    }

    public void setNavButtonOrder(List<NavButton> order) {
        storage.write(KEY_NAVBAR_BUTTON_ORDER, ItemOrder.format(order, button -> button.id));
        notifyNavbarListeners();
    }

    /** Called whenever the clock format or what the navbar shows is changed, so the navbar can follow it. */
    public void addNavbarListener(Runnable listener) {
        navbarListeners.add(listener);
    }

    /** Whether the launcher's own screensaver runs when it sits idle. On until the user turns it off. */
    public boolean isScreensaverOn() {
        return !"0".equals(storage.read(KEY_SCREENSAVER_ON));
    }

    public void setScreensaverOn(boolean on) {
        storage.write(KEY_SCREENSAVER_ON, on ? "1" : "0");
        notifyScreensaverListeners();
    }

    /** Whether the screensaver cycles by itself, and through which pictures. */
    public BackgroundRandom screensaverRandom() {
        return BackgroundRandom.fromId(storage.read(KEY_SCREENSAVER_BACKGROUND));
    }

    public boolean isScreensaverRandom() {
        return screensaverRandom() != BackgroundRandom.OFF;
    }

    public void setScreensaverRandom(BackgroundRandom mode) {
        storage.write(KEY_SCREENSAVER_BACKGROUND, mode.id == null ? screensaverBackground().id : mode.id);
        notifyScreensaverListeners();
    }

    /** The screensaver's own background (not used while random is on); Mountains until chosen. */
    public AppBackground screensaverBackground() {
        return AppBackground.fromId(storage.read(KEY_SCREENSAVER_BACKGROUND));
    }

    public void setScreensaverBackground(AppBackground background) {
        storage.write(KEY_SCREENSAVER_BACKGROUND, background.id);
        notifyScreensaverListeners();
    }

    public boolean isScreensaverWidgetShown(ScreensaverWidget widget) {
        String saved = storage.read(KEY_SCREENSAVER_WIDGET_PREFIX + widget.id);
        return saved == null ? widget.shownByDefault : "1".equals(saved);
    }

    public void setScreensaverWidgetShown(ScreensaverWidget widget, boolean shown) {
        storage.write(KEY_SCREENSAVER_WIDGET_PREFIX + widget.id, shown ? "1" : "0");
        notifyScreensaverListeners();
    }

    /** Whether the screensaver's background slowly moves; the launcher's own background never does. On by default. */
    public boolean isScreensaverMotion() {
        return !"0".equals(storage.read(KEY_SCREENSAVER_MOTION));
    }

    public void setScreensaverMotion(boolean on) {
        storage.write(KEY_SCREENSAVER_MOTION, on ? "1" : "0");
        notifyScreensaverListeners();
    }

    public ScreensaverDelay screensaverDelay() {
        return ScreensaverDelay.fromId(storage.read(KEY_SCREENSAVER_DELAY));
    }

    public void setScreensaverDelay(ScreensaverDelay delay) {
        storage.write(KEY_SCREENSAVER_DELAY, delay.id);
        notifyScreensaverListeners();
    }

    /**
     * Keeps the settings sensible after an uploaded picture was deleted: anything that was set to show it goes back
     * to the default background, and so does a "random custom" left with no pictures.
     * @param anyLeft whether any uploaded picture remains
     */
    public void customBackgroundDeleted(AppBackground removed, boolean anyLeft) {
        if (background() == removed || (!anyLeft && backgroundRandom() == BackgroundRandom.CUSTOM)) {
            storage.write(KEY_BACKGROUND, AppBackground.DEFAULT.id);
        }
        if (screensaverBackground() == removed || (!anyLeft && screensaverRandom() == BackgroundRandom.CUSTOM)) {
            storage.write(KEY_SCREENSAVER_BACKGROUND, AppBackground.DEFAULT.id);
        }
        customBackgroundChanged();
        notifyBackgroundListeners();
        notifyScreensaverListeners();
    }

    /** Called after a new picture was uploaded, so every list and screen showing it can pick it up. */
    public void addCustomBackgroundListener(Runnable listener) {
        customBackgroundListeners.add(listener);
    }

    public void customBackgroundChanged() {
        for (Runnable listener : new ArrayList<>(customBackgroundListeners)) {
            listener.run();
        }
    }

    /** Called whenever a screensaver setting is changed, so the idle timer can follow it. */
    public void addScreensaverListener(Runnable listener) {
        screensaverListeners.add(listener);
    }

    private void notifyScreensaverListeners() {
        for (Runnable listener : new ArrayList<>(screensaverListeners)) {
            listener.run();
        }
    }

    private void notifyNavbarListeners() {
        for (Runnable listener : new ArrayList<>(navbarListeners)) {
            listener.run();
        }
    }
}
