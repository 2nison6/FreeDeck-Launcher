package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.ScreensaverWidget;

import java.util.ArrayList;
import java.util.List;

/** A show / hide switch for each widget the screensaver can display. */
class ScreensaverWidgetsPanel extends TileGrid {

    ScreensaverWidgetsPanel(Context context, AppSettings settings, Runnable onChanged) {
        super(context);
        List<View> rows = new ArrayList<>();
        for (ScreensaverWidget widget : ScreensaverWidget.values()) {
            SettingRow row = new SettingRow(context);
            row.setTitle(widget.label);
            row.setIcon(R.drawable.ic_setting_visibility);
            row.setChevronVisible(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = context.getResources().getDimensionPixelSize(R.dimen.settings_row_gap);
            row.setLayoutParams(params);
            row.setSwitch(settings.isScreensaverWidgetShown(widget));
            row.setOnClickListener(v -> {
                boolean shown = !settings.isScreensaverWidgetShown(widget);
                settings.setScreensaverWidgetShown(widget, shown);
                row.setSwitch(shown);
                onChanged.run();
            });
            rows.add(row);
        }
        setTiles(rows, 1, rows.get(0));
    }
}
