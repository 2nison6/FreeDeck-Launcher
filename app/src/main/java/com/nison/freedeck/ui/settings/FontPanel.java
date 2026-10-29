package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.FontStyle;
import com.nison.freedeck.data.settings.ThemeMode;
import com.nison.freedeck.ui.common.TextStyler;

import java.util.ArrayList;
import java.util.List;

/** Pick a typeface (each shown in itself) and a text colour for Dark mode and another for Light mode. */
class FontPanel extends LinearLayout implements DropdownContent {

    private static final int FONT_COLUMNS = 4;

    private final ChoiceGrid fonts;
    private final List<List<View>> rows = new ArrayList<>();
    private View upExit;
    private View downExit;

    /** @param onChanged called after a choice changed; it rebuilds the screen so the text restyles */
    FontPanel(Context context, AppSettings settings, Runnable onChanged) {
        super(context);
        setOrientation(VERTICAL);

        FontStyle[] styles = FontStyle.values();
        int[] fontLabels = new int[styles.length];
        for (int i = 0; i < styles.length; i++) {
            fontLabels[i] = styles[i].label;
        }
        fonts = new ChoiceGrid(context, fontLabels, settings.fontStyle().ordinal(), FONT_COLUMNS,
                new ChoiceGrid.Listener() {
                    @Override
                    public void onFocused(int index) {
                    }

                    @Override
                    public void onChosen(int index) {
                        if (styles[index] != settings.fontStyle()) {
                            settings.setFontStyle(styles[index]);
                            onChanged.run();
                        }
                    }
                }, (index, chip) -> {
                    Typeface face = TextStyler.load(context, styles[index]);
                    if (face != null) {
                        chip.setTypeface(face);
                    }
                });

        addView(heading(context, R.string.font_heading_style));
        addView(fonts);

        ModeColorPicker dark = new ModeColorPicker(context, ThemeMode.DARK, R.string.font_color_dark, settings, onChanged);
        ModeColorPicker light = new ModeColorPicker(context, ThemeMode.LIGHT, R.string.font_color_light, settings, onChanged);
        addView(dark);
        addView(light);
        rows.addAll(dark.focusRows());
        rows.addAll(light.focusRows());
        for (int r = 0; r < rows.size(); r++) {
            for (View view : rows.get(r)) {
                int row = r;
                view.setOnKeyListener((v, keyCode, event) -> onPickerKey(v, row, keyCode, event));
            }
        }
        fonts.setExits(null, rows.get(0).get(0));
    }

    private static TextView heading(Context context, int text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(context.getColor(R.color.text_secondary));
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.chip_text));
        view.setPadding(0, context.getResources().getDimensionPixelSize(R.dimen.chip_gap),
                0, context.getResources().getDimensionPixelSize(R.dimen.chip_gap));
        return view;
    }

    /** Up and down move between the bars and buttons; the sliders handle left and right themselves. */
    private boolean onPickerKey(View view, int row, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        List<View> here = rows.get(row);
        int column = here.indexOf(view);
        View target;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                target = row == rows.size() - 1 ? downExit : pick(rows.get(row + 1), column);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                target = row == 0 ? fonts.preferredFocus() : pick(rows.get(row - 1), column);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (here.size() == 1) {
                    return false; // a slider: it moves its own marker
                }
                target = here.get((column + here.size() - 1) % here.size());
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (here.size() == 1) {
                    return false;
                }
                target = here.get((column + 1) % here.size());
                break;
            default:
                return false;
        }
        if (target != null) {
            target.requestFocus();
        }
        return true;
    }

    private static View pick(List<View> row, int column) {
        return row.get(Math.min(column, row.size() - 1));
    }

    @Override
    public View view() {
        return this;
    }

    @Override
    public View preferredFocus() {
        return fonts.preferredFocus();
    }

    @Override
    public void setExits(View up, View down) {
        upExit = up;
        downExit = down;
        fonts.setExits(up, rows.get(0).get(0));
    }
}
