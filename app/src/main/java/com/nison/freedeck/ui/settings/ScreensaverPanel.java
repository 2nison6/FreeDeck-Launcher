package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.system.SystemScreensaver;

/**
 * Everything about the screensaver under one setting: the on / off switch, the wait time, and two sections that open
 * right under their row, like the launcher's own settings do: Background (closes once one is chosen) and Widgets.
 */
class ScreensaverPanel extends LinearLayout implements DropdownContent {

    /** A row with an optional section that opens under it. */
    private static final class Section {
        final SettingRow row;
        final FrameLayout container;
        final DropdownContent content;

        Section(SettingRow row, FrameLayout container, DropdownContent content) {
            this.row = row;
            this.container = container;
            this.content = content;
        }

        boolean isOpen() {
            return container.getVisibility() == VISIBLE;
        }
    }

    private final SettingRow enabled;
    private final SettingRow delay;
    private final SettingRow motion;
    private final Section widgets;
    private View upExit;
    private View downExit;

    ScreensaverPanel(Context context, AppSettings settings, Runnable onChanged) {
        super(context);
        setOrientation(VERTICAL);

        enabled = row(context, R.string.screensaver_enabled);
        enabled.setSwitch(settings.isScreensaverOn());
        enabled.setOnClickListener(v -> {
            boolean on = !settings.isScreensaverOn();
            settings.setScreensaverOn(on);
            enabled.setSwitch(on);
            SystemScreensaver.followLauncher(context, on);
            onChanged.run();
        });
        addView(enabled);
        // Also turned on from elsewhere (the default-launcher popup): keep the switch showing the truth.
        settings.addScreensaverListener(() -> enabled.setSwitch(settings.isScreensaverOn()));

        delay = row(context, R.string.screensaver_delay);
        delay.setValue(context.getString(settings.screensaverDelay().label));
        delay.setOnClickListener(v -> {
            settings.setScreensaverDelay(settings.screensaverDelay().next());
            delay.setValue(context.getString(settings.screensaverDelay().label));
            onChanged.run();
        });
        addView(delay);

        motion = row(context, R.string.screensaver_motion);
        motion.setSwitch(settings.isScreensaverMotion());
        motion.setOnClickListener(v -> {
            boolean on = !settings.isScreensaverMotion();
            settings.setScreensaverMotion(on);
            motion.setSwitch(on);
            onChanged.run();
        });
        addView(motion);

        SettingRow backgroundRow = row(context, R.string.setting_screensaver_background);
        backgroundRow.setChevronVisible(true);
        FrameLayout backgroundContainer = container(context);
        Section[] holder = new Section[1];
        DropdownContent backgroundGrid = new SettingsDropdowns.BackgroundGrid(context, settings, () -> {
            // Choosing a background closes its section and returns to its row.
            close(holder[0]);
            holder[0].row.requestFocus();
            refreshBackgroundValue(context, settings, holder[0].row);
            onChanged.run();
        }, true);
        holder[0] = new Section(backgroundRow, backgroundContainer, backgroundGrid);
        refreshBackgroundValue(context, settings, backgroundRow);
        settings.addCustomBackgroundListener(() -> refreshBackgroundValue(context, settings, backgroundRow));
        settings.addScreensaverListener(() -> refreshBackgroundValue(context, settings, backgroundRow));
        backgroundContainer.addView(backgroundGrid.view());
        addView(backgroundRow);
        addView(backgroundContainer);

        SettingRow widgetsRow = row(context, R.string.setting_screensaver_widgets);
        widgetsRow.setChevronVisible(true);
        FrameLayout widgetsContainer = container(context);
        DropdownContent widgetsPanel = new ScreensaverWidgetsPanel(context, settings, onChanged);
        widgetsContainer.addView(widgetsPanel.view());
        widgets = new Section(widgetsRow, widgetsContainer, widgetsPanel);
        addView(widgetsRow);
        addView(widgetsContainer);

        backgroundSectionRef = holder[0];
        wire();
    }

    private Section backgroundSectionRef;

    private static void refreshBackgroundValue(Context context, AppSettings settings, SettingRow row) {
        switch (settings.screensaverRandom()) {
            case SYSTEM:
                row.setValue(context.getString(R.string.screensaver_random_system));
                break;
            case CUSTOM:
                row.setValue(context.getString(R.string.screensaver_random_custom));
                break;
            default:
                row.setValue(context.getString(settings.screensaverBackground().label));
        }
    }

    private static SettingRow row(Context context, int title) {
        SettingRow row = new SettingRow(context);
        row.setTitle(title);
        row.setIcon(R.drawable.ic_setting_tv);
        row.setChevronVisible(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.settings_row_gap);
        row.setLayoutParams(params);
        return row;
    }

    private static FrameLayout container(Context context) {
        FrameLayout container = new FrameLayout(context);
        container.setPadding(context.getResources().getDimensionPixelSize(R.dimen.dropdown_indent), 0, 0,
                context.getResources().getDimensionPixelSize(R.dimen.settings_row_gap));
        container.setVisibility(GONE);
        return container;
    }

    private static void close(Section section) {
        section.container.setVisibility(GONE);
    }

    private void toggle(Section section) {
        boolean opening = !section.isOpen();
        close(backgroundSectionRef);
        close(widgets);
        if (opening) {
            section.container.setVisibility(VISIBLE);
        }
    }

    /** The row that follows {@code section}'s own, below its open options. */
    private View after(View row) {
        if (row == enabled) {
            return delay;
        }
        if (row == delay) {
            return motion;
        }
        if (row == motion) {
            return backgroundSectionRef.row;
        }
        if (row == backgroundSectionRef.row) {
            return widgets.row;
        }
        return downExit;
    }

    private View before(View row) {
        if (row == widgets.row) {
            return backgroundSectionRef.row;
        }
        if (row == backgroundSectionRef.row) {
            return motion;
        }
        if (row == motion) {
            return delay;
        }
        if (row == delay) {
            return enabled;
        }
        return upExit;
    }

    private void wire() {
        for (Section section : new Section[]{backgroundSectionRef, widgets}) {
            section.row.setOnClickListener(v -> toggle(section));
            section.content.setExits(section.row, after(section.row));
        }
        for (View row : new View[]{enabled, delay, motion, backgroundSectionRef.row, widgets.row}) {
            row.setOnKeyListener((v, keyCode, event) -> onRowKey(v, keyCode, event));
        }
    }

    private boolean onRowKey(View row, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        View target;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Section section = row == backgroundSectionRef.row ? backgroundSectionRef
                        : row == widgets.row ? widgets : null;
                target = section != null && section.isOpen() ? section.content.preferredFocus() : after(row);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                target = before(row);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return true;
            default:
                return false;
        }
        if (target != null) {
            target.requestFocus();
        }
        return true;
    }

    @Override
    public View view() {
        return this;
    }

    @Override
    public View preferredFocus() {
        return enabled;
    }

    @Override
    public void setExits(View up, View down) {
        upExit = up;
        downExit = down;
        // The last section's options leave downwards to whatever follows the whole panel.
        widgets.content.setExits(widgets.row, downExit);
    }
}
