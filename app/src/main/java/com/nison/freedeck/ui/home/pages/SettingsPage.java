package com.nison.freedeck.ui.home.pages;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.system.DefaultLauncher;
import com.nison.freedeck.system.SystemSettings;
import com.nison.freedeck.ui.main.LauncherSession;
import com.nison.freedeck.ui.settings.DropdownContent;
import com.nison.freedeck.ui.settings.SettingRow;
import com.nison.freedeck.ui.settings.SettingsDropdowns;
import com.nison.freedeck.ui.sound.FocusSound;

import java.util.ArrayList;
import java.util.List;

/**
 * The launcher's own settings. Each is a row; selecting it opens its options right under it (and selecting it again
 * closes them), and moving down goes into them. They stay open while moving between settings, and close when an
 * option is chosen or focus leaves the page. A button at the end
 * opens Fire TV's own Settings.
 */
public class SettingsPage extends FrameLayout {

    /** One setting: its row, the closed-or-open area under it, and what goes in that area. */
    private static final class Entry {
        final SettingRow row;
        final ViewGroup container;
        DropdownContent content;

        Entry(SettingRow row, ViewGroup container) {
            this.row = row;
            this.container = container;
        }

        boolean owns(View view) {
            if (view == null) {
                return false;
            }
            for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
                if (v == row || v == container) {
                    return true;
                }
            }
            return false;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final View moreButton;
    private final SettingRow defaultLauncherRow;
    private AppSettings appSettings;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener =
            (oldFocus, newFocus) -> closeUnlessFocusIn(newFocus);

    public SettingsPage(Context context) {
        this(context, null);
    }

    public SettingsPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.page_settings, this, true);
        entries.add(new Entry(findViewById(R.id.setting_navigation_sound), findViewById(R.id.dropdown_navigation_sound)));
        entries.add(new Entry(findViewById(R.id.setting_apps_layout), findViewById(R.id.dropdown_apps_layout)));
        entries.add(new Entry(findViewById(R.id.setting_app_theme), findViewById(R.id.dropdown_app_theme)));
        entries.add(new Entry(findViewById(R.id.setting_background), findViewById(R.id.dropdown_background)));
        entries.add(new Entry(findViewById(R.id.setting_language), findViewById(R.id.dropdown_language)));
        entries.add(new Entry(findViewById(R.id.setting_font), findViewById(R.id.dropdown_font)));
        entries.add(new Entry(findViewById(R.id.setting_navbar), findViewById(R.id.dropdown_navbar)));
        entries.add(new Entry(findViewById(R.id.setting_reorder), findViewById(R.id.dropdown_reorder)));
        entries.add(new Entry(findViewById(R.id.setting_screensaver), findViewById(R.id.dropdown_screensaver)));
        defaultLauncherRow = findViewById(R.id.setting_default_launcher);
        defaultLauncherRow.setChevronVisible(false);
        moreButton = findViewById(R.id.settings_more);
        moreButton.setOnClickListener(v -> SystemSettings.openMain(context));
    }

    public void bind(AppSettings settings, FocusSound focusSound) {
        Context context = getContext();
        appSettings = settings;
        entries.get(0).content = SettingsDropdowns.sound(context, settings, focusSound, () -> refreshValues(settings));
        entries.get(1).content = SettingsDropdowns.appsLayout(context, settings, () -> refreshValues(settings));
        entries.get(2).content = SettingsDropdowns.theme(context, settings,
                () -> LauncherSession.restartForTheme(context));
        entries.get(3).content = SettingsDropdowns.background(context, settings, () -> refreshValues(settings));
        entries.get(4).content = SettingsDropdowns.language(context, settings, () -> LauncherSession.restartForTheme(context));
        entries.get(5).content = SettingsDropdowns.font(context, settings, () -> LauncherSession.restartForTheme(context));
        entries.get(6).content = SettingsDropdowns.navbar(context, settings, () -> refreshValues(settings));
        entries.get(7).content = SettingsDropdowns.reorder(context, settings, () -> refreshValues(settings));
        entries.get(8).content = SettingsDropdowns.screensaver(context, settings, () -> refreshValues(settings));
        for (Entry entry : entries) {
            entry.container.removeAllViews();
            entry.container.addView(entry.content.view());
        }
        refreshValues(settings);
        settings.addScreensaverListener(() -> refreshValues(settings));
        refreshDefaultLauncherSwitch();
        wireFocus();
    }

    /** The current choice shown on the right of each row. */
    private void refreshValues(AppSettings settings) {
        Context context = getContext();
        entries.get(0).row.setValue(context.getString(settings.navigationSound().label));
        entries.get(1).row.setValue(context.getString(settings.appLayout().label));
        entries.get(2).row.setValue(context.getString(settings.themeMode().label));
        switch (settings.backgroundRandom()) {
            case SYSTEM:
                entries.get(3).row.setValue(context.getString(R.string.screensaver_random_system));
                break;
            case CUSTOM:
                entries.get(3).row.setValue(context.getString(R.string.screensaver_random_custom));
                break;
            default:
                entries.get(3).row.setValue(context.getString(settings.background().label));
        }
        entries.get(4).row.setValue(settings.language().nativeName);
        entries.get(5).row.setValue(context.getString(settings.fontStyle().label));
        entries.get(8).row.setValue(context.getString(settings.isScreensaverOn() ? R.string.switch_on : R.string.switch_off));
    }

    /** Up and down move between rows and into the open dropdown; left and right never leave the page's column. */
    private void wireFocus() {
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            View above = i == 0 ? defaultLauncherRow : entries.get(i - 1).row;
            View below = i == entries.size() - 1 ? moreButton : entries.get(i + 1).row;
            entry.row.setOnKeyListener((v, keyCode, event) -> onRowKey(entry, above, below, keyCode, event));
            entry.row.setOnClickListener(v -> toggle(entry));
            entry.content.setExits(entry.row, below);
        }
        defaultLauncherRow.setOnClickListener(v -> toggleDefaultLauncher());
        defaultLauncherRow.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    View nav = getRootView().findViewById(R.id.nav_settings);
                    if (nav != null) {
                        nav.requestFocus();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    entries.get(0).row.requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return true;
                default:
                    return false;
            }
        });
        moreButton.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    entries.get(entries.size() - 1).row.requestFocus();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return true;
                default:
                    return false;
            }
        });
    }

    private boolean onRowKey(Entry entry, View above, View below, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                (entry.container.getVisibility() == VISIBLE ? entry.content.preferredFocus() : below).requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                View target = above != null ? above : getRootView().findViewById(R.id.nav_settings);
                if (target != null) {
                    target.requestFocus();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return true;
            default:
                return false;
        }
    }

    /** The switch shows on only when it was turned on and the Home-button service really is running. */
    public void refreshDefaultLauncherSwitch() {
        defaultLauncherRow.setSwitch(isDefaultLauncherOn());
    }

    private boolean isDefaultLauncherOn() {
        return appSettings != null && appSettings.isDefaultLauncher()
                && DefaultLauncher.isServiceEnabled(getContext());
    }

    /** On: Home opens this launcher. Off: Home goes to Fire TV's own, as before. */
    private void toggleDefaultLauncher() {
        if (isDefaultLauncherOn()) {
            appSettings.setDefaultLauncher(false);
        } else {
            appSettings.setDefaultLauncher(true);
            // The permission may have to be asked for first, so the switch settles once the answer is in.
            com.nison.freedeck.ui.main.AdbHelpDialog.enableOrExplain(getContext(), this::refreshDefaultLauncherSwitch);
        }
        refreshDefaultLauncherSwitch();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE && appSettings != null) {
            // Show the switch as it really is now.
            refreshDefaultLauncherSwitch();
        }
    }

    /** Opens the setting's options and moves into them, or closes them if they are open. */
    private void toggle(Entry entry) {
        boolean opening = entry.container.getVisibility() != VISIBLE;
        for (Entry other : entries) {
            other.container.setVisibility(other == entry && opening ? VISIBLE : GONE);
        }
        if (opening) {
            scrollToTop(entry.row);
        }
    }

    /** Once the layout has settled, scrolls so the setting sits at the top and everything above leaves the view. */
    private void scrollToTop(View row) {
        ViewTreeObserver observer = row.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewTreeObserver current = row.getViewTreeObserver();
                if (current.isAlive()) {
                    current.removeOnGlobalLayoutListener(this);
                }
                ScrollView scroll = findViewById(R.id.settings_scroll);
                scroll.smoothScrollTo(0, row.getTop());
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        View space = findViewById(R.id.settings_end_space);
        int height = Math.max(0, h - getResources().getDimensionPixelSize(R.dimen.settings_row_height) * 2);
        if (space.getLayoutParams().height != height) {
            space.getLayoutParams().height = height;
            space.requestLayout();
        }
    }

    /** Focus left the Settings page altogether (e.g. up to the nav bar): close whatever is open. */
    private void closeUnlessFocusIn(View focused) {
        if (focused == null || isInside(focused)) {
            return;
        }
        for (Entry entry : entries) {
            entry.container.setVisibility(GONE);
        }
    }

    private boolean isInside(View view) {
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (v == this) {
                return true;
            }
        }
        return false;
    }

    /** Choosing an option inside a dropdown closes it and returns to its setting (the Navbar switches stay open). */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View before = findFocus();
        boolean handled = super.dispatchKeyEvent(event);
        boolean select = event.getAction() == KeyEvent.ACTION_UP
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
        if (select && before != null) {
            for (int i = 0; i < 6; i++) {
                Entry entry = entries.get(i);
                if (entry.container.getVisibility() == VISIBLE && entry.owns(before) && before != entry.row) {
                    entry.container.setVisibility(GONE);
                    entry.row.requestFocus();
                }
            }
        }
        return handled;
    }

    /** Puts focus on the theme row, e.g. after the screen was rebuilt in the other mode. */
    public void focusThemeRow() {
        entries.get(2).row.requestFocus();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
        }
        super.onDetachedFromWindow();
    }
}
