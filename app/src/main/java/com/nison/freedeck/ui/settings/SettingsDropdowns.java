package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppBackground;
import com.nison.freedeck.data.settings.AppLanguage;
import com.nison.freedeck.data.settings.AppLayout;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.ClockFormat;
import com.nison.freedeck.data.settings.CustomBackground;
import com.nison.freedeck.data.settings.NavbarItem;
import com.nison.freedeck.data.settings.NavigationSound;
import com.nison.freedeck.data.settings.BackgroundRandom;
import com.nison.freedeck.data.settings.ThemeMode;
import com.nison.freedeck.ui.common.BackgroundDrawables;
import com.nison.freedeck.ui.main.LauncherSession;
import com.nison.freedeck.ui.sound.FocusSound;

import java.util.ArrayList;
import java.util.List;

/** The dropdowns that open under each setting. */
public final class SettingsDropdowns {

    private SettingsDropdowns() {
    }

    /** The navigation sounds; moving onto one plays it, selecting one keeps it. */
    public static DropdownContent sound(Context context, AppSettings settings, FocusSound focusSound,
                                        Runnable onChanged) {
        NavigationSound[] sounds = NavigationSound.values();
        return new ChoiceGrid(context, labelsOf(sounds), settings.navigationSound().ordinal(), 5,
                new ChoiceGrid.Listener() {
                    @Override
                    public void onFocused(int index) {
                        focusSound.preview(sounds[index]);
                    }

                    @Override
                    public void onChosen(int index) {
                        settings.setNavigationSound(sounds[index]);
                        onChanged.run();
                    }
                });
    }

    public static DropdownContent appsLayout(Context context, AppSettings settings, Runnable onChanged) {
        AppLayout[] layouts = AppLayout.values();
        int[] labels = new int[layouts.length];
        for (int i = 0; i < layouts.length; i++) {
            labels[i] = layouts[i].label;
        }
        return new ChoiceGrid(context, labels, settings.appLayout().ordinal(), 3, new ChoiceGrid.Listener() {
            @Override
            public void onFocused(int index) {
            }

            @Override
            public void onChosen(int index) {
                settings.setAppLayout(layouts[index]);
                onChanged.run();
            }
        });
    }

    /** @param onChanged called only when a different mode than the current one was chosen */
    public static DropdownContent theme(Context context, AppSettings settings, Runnable onChanged) {
        ThemeMode[] modes = ThemeMode.values();
        int[] labels = new int[modes.length];
        for (int i = 0; i < modes.length; i++) {
            labels[i] = modes[i].label;
        }
        return new ChoiceGrid(context, labels, settings.themeMode().ordinal(), 2, new ChoiceGrid.Listener() {
            @Override
            public void onFocused(int index) {
            }

            @Override
            public void onChosen(int index) {
                if (modes[index] != settings.themeMode()) {
                    settings.setThemeMode(modes[index]);
                    onChanged.run();
                }
            }
        });
    }

    /** Previews of the backgrounds, then a "+" tile to add your own picture. */
    public static DropdownContent background(Context context, AppSettings settings, Runnable onChanged) {
        return new BackgroundGrid(context, settings, onChanged, false);
    }

    /** Every language the launcher is available in, each in its own script; choosing one rebuilds the screen. */
    public static DropdownContent language(Context context, AppSettings settings, Runnable onChanged) {
        AppLanguage[] languages = AppLanguage.values();
        return new LanguageGrid(context, languages, settings.language().ordinal(), new ChoiceGrid.Listener() {
            @Override
            public void onFocused(int index) {
            }

            @Override
            public void onChosen(int index) {
                if (languages[index] != settings.language()) {
                    settings.setLanguage(languages[index]);
                    onChanged.run();
                }
            }
        });
    }

    /** A typeface and a text colour; choosing either rebuilds the screen so all text follows. */
    public static DropdownContent font(Context context, AppSettings settings, Runnable onChanged) {
        return new FontPanel(context, settings, onChanged);
    }

    /** A live preview of the navbar and two lists to put its text and buttons in the order wanted. */
    public static DropdownContent reorder(Context context, AppSettings settings, Runnable onChanged) {
        return new ReorderPanel(context, settings, onChanged);
    }

    /** The clock format and a show / hide switch for each piece of the navbar. */
    public static DropdownContent navbar(Context context, AppSettings settings, Runnable onChanged) {
        return new NavbarList(context, settings, onChanged);
    }

    /** The screensaver's on / off switch, its style and its wait time. */
    public static DropdownContent screensaver(Context context, AppSettings settings, Runnable onChanged) {
        return new ScreensaverPanel(context, settings, onChanged);
    }

    private static int[] labelsOf(NavigationSound[] sounds) {
        int[] labels = new int[sounds.length];
        for (int i = 0; i < sounds.length; i++) {
            labels[i] = sounds[i].label;
        }
        return labels;
    }

    // ------------------------------------------------------------------------------------------------

    static final class BackgroundGrid extends TileGrid {

        private static final int COLUMNS = 4;

        private final AppSettings settings;
        private final Runnable onChanged;
        private final List<AppBackground> shown = new ArrayList<>();
        private final List<View> backgroundTiles = new ArrayList<>();
        private final boolean forScreensaver;
        private View randomSystemTile;
        private View randomCustomTile;

        BackgroundGrid(Context context, AppSettings settings, Runnable onChanged, boolean forScreensaver) {
            super(context);
            this.settings = settings;
            this.onChanged = onChanged;
            this.forScreensaver = forScreensaver;
            rebuild();
            // An upload made from the other Background list changes the Custom tiles here too.
            settings.addCustomBackgroundListener(this::rebuild);
        }

        private AppBackground current() {
            return forScreensaver ? settings.screensaverBackground() : settings.background();
        }

        private BackgroundRandom mode() {
            return forScreensaver ? settings.screensaverRandom() : settings.backgroundRandom();
        }

        private void chooseRandom(BackgroundRandom mode) {
            if (forScreensaver) {
                settings.setScreensaverRandom(mode);
            } else {
                settings.setBackgroundRandom(mode);
            }
        }

        private void choose(AppBackground background) {
            if (forScreensaver) {
                settings.setScreensaverBackground(background);
            } else {
                settings.setBackground(background);
            }
        }

        /** Lays out every tile again, landing on the one in use. */
        private void rebuild() {
            shown.clear();
            backgroundTiles.clear();
            List<View> tiles = new ArrayList<>();
            View preferred = null;
            randomSystemTile = null;
            randomCustomTile = null;
            BackgroundRandom mode = mode();
            {
                randomSystemTile = createRandomTile(R.string.screensaver_random_system, BackgroundRandom.SYSTEM);
                randomCustomTile = createRandomTile(R.string.screensaver_random_custom, BackgroundRandom.CUSTOM);
                tiles.add(randomSystemTile);
                tiles.add(randomCustomTile);
                preferred = mode == BackgroundRandom.SYSTEM ? randomSystemTile
                        : mode == BackgroundRandom.CUSTOM ? randomCustomTile : null;
            }
            for (AppBackground background : AppBackground.values()) {
                if (background.isCustom() && !CustomBackground.exists(getContext(), background.slot())) {
                    continue;
                }
                View tile = createTile(background);
                shown.add(background);
                backgroundTiles.add(tile);
                tiles.add(tile);
                if (background == current() && mode == BackgroundRandom.OFF) {
                    preferred = tile;
                }
            }
            if (!forScreensaver) {
                // Uploads are made from the launcher's own list; the screensaver just uses what is there.
                tiles.add(createAddTile());
            }
            setTiles(tiles, COLUMNS, preferred);
            refreshChecks();
        }

        private View createTile(AppBackground background) {
            View tile = LayoutInflater.from(getContext()).inflate(R.layout.item_background_tile, null, false);
            ImageView thumb = tile.findViewById(R.id.bg_thumb);
            if (background.isNone()) {
                tile.findViewById(R.id.bg_none).setVisibility(VISIBLE);
            } else {
                Drawable preview = BackgroundDrawables.preview(getContext(), background);
                if (preview != null) {
                    thumb.setImageDrawable(preview);
                }
            }
            ((TextView) tile.findViewById(R.id.bg_label)).setText(background.label);
            tile.setOnClickListener(v -> {
                choose(background);
                refreshChecks();
                onChanged.run();
            });
            if (background.isCustom() && !forScreensaver) {
                // Holding Select on an uploaded picture offers to delete it (the screensaver list only follows).
                tile.setLongClickable(true);
                tile.setOnLongClickListener(v -> {
                    new ConfirmDeleteDialog(getContext(), getContext().getString(background.label),
                            () -> deleteCustom(background)).show();
                    return true;
                });
            }
            tile.setOnFocusChangeListener((v, hasFocus) -> grow(v, hasFocus));
            return tile;
        }

        private View createRandomTile(int label, BackgroundRandom mode) {
            View tile = LayoutInflater.from(getContext()).inflate(R.layout.item_background_tile, null, false);
            ImageView icon = tile.findViewById(R.id.bg_none);
            icon.setImageResource(R.drawable.ic_shuffle);
            icon.setVisibility(VISIBLE);
            ((TextView) tile.findViewById(R.id.bg_label)).setText(label);
            tile.setOnClickListener(v -> {
                chooseRandom(mode);
                refreshChecks();
                onChanged.run();
            });
            tile.setOnFocusChangeListener((v, hasFocus) -> grow(v, hasFocus));
            return tile;
        }

        private View createAddTile() {
            View tile = LayoutInflater.from(getContext()).inflate(R.layout.item_background_tile, null, false);
            ImageView plus = tile.findViewById(R.id.bg_none);
            plus.setImageResource(R.drawable.ic_add);
            plus.setVisibility(VISIBLE);
            tile.findViewById(R.id.bg_label).setVisibility(INVISIBLE);
            tile.setContentDescription(getContext().getString(R.string.background_add));
            tile.setOnClickListener(v -> {
                int slot = CustomBackground.slotForUpload(getContext());
                PhoneUploadDialogs.show(getContext(), slot, () -> onUploaded(slot));
            });
            tile.setOnFocusChangeListener((v, hasFocus) -> grow(v, hasFocus));
            return tile;
        }

        /** Opens on the background in use now, not the one that was in use when the list was built. */
        @Override
        public View preferredFocus() {
            {
                BackgroundRandom mode = mode();
                if (mode == BackgroundRandom.SYSTEM) {
                    return randomSystemTile;
                }
                if (mode == BackgroundRandom.CUSTOM) {
                    return randomCustomTile;
                }
            }
            int index = shown.indexOf(current());
            return index >= 0 ? backgroundTiles.get(index) : super.preferredFocus();
        }

        /** Removes the picture, then lets every list and screen showing it follow at once. */
        private void deleteCustom(AppBackground background) {
            CustomBackground.delete(getContext(), background.slot());
            settings.customBackgroundDeleted(background, CustomBackground.count(getContext()) > 0);
            preferredFocus().requestFocus();
            onChanged.run();
        }

        /** A picture from the phone was saved as Custom {@code slot}: use it, show it in both lists, and land on it. */
        private void onUploaded(int slot) {
            choose(AppBackground.forSlot(slot));
            settings.customBackgroundChanged(); // rebuilds this list and the screensaver's
            preferredFocus().requestFocus();
            onChanged.run();
        }

        private void refreshChecks() {
            AppBackground current = current();
            BackgroundRandom mode = mode();
            if (randomSystemTile != null) {
                randomSystemTile.findViewById(R.id.bg_check)
                        .setVisibility(mode == BackgroundRandom.SYSTEM ? VISIBLE : GONE);
                randomCustomTile.findViewById(R.id.bg_check)
                        .setVisibility(mode == BackgroundRandom.CUSTOM ? VISIBLE : GONE);
            }
            for (int i = 0; i < backgroundTiles.size(); i++) {
                backgroundTiles.get(i).findViewById(R.id.bg_check)
                        .setVisibility(mode == BackgroundRandom.OFF && shown.get(i) == current ? VISIBLE : GONE);
            }
        }

        private static void grow(View tile, boolean hasFocus) {
            float scale = hasFocus ? 1.06f : 1f;
            tile.animate().scaleX(scale).scaleY(scale).setDuration(120L).start();
        }
    }

    // ------------------------------------------------------------------------------------------------

    private static final class NavbarList extends TileGrid {

        NavbarList(Context context, AppSettings settings, Runnable onChanged) {
            super(context);
            List<View> rows = new ArrayList<>();

            SettingRow clock = row(context, R.string.setting_clock_format, R.drawable.ic_setting_clock);
            clock.setValue(context.getString(settings.clockFormat().label));
            clock.setOnClickListener(v -> {
                ClockFormat next = settings.clockFormat() == ClockFormat.HOURS_12
                        ? ClockFormat.HOURS_24 : ClockFormat.HOURS_12;
                settings.setClockFormat(next);
                clock.setValue(context.getString(next.label));
                onChanged.run();
            });
            rows.add(clock);

            for (NavbarItem item : NavbarItem.values()) {
                SettingRow row = row(context, item.label, R.drawable.ic_setting_visibility);
                row.setSwitch(settings.isNavbarItemShown(item));
                row.setOnClickListener(v -> {
                    boolean shownNow = !settings.isNavbarItemShown(item);
                    settings.setNavbarItemShown(item, shownNow);
                    row.setSwitch(shownNow);
                    onChanged.run();
                });
                rows.add(row);
            }
            setTiles(rows, 1, rows.get(0));
        }

        private static SettingRow row(Context context, int title, int icon) {
            SettingRow row = new SettingRow(context);
            row.setTitle(title);
            row.setIcon(icon);
            row.setChevronVisible(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.settings_row_gap);
            row.setLayoutParams(params);
            return row;
        }
    }
}
