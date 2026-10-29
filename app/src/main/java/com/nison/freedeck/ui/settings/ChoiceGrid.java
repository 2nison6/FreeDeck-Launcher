package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.ui.common.TextStyler;

import java.util.ArrayList;
import java.util.List;

/** A set of short text options in rows; the chosen one is outlined, and selecting another one chooses it. */
class ChoiceGrid extends TileGrid {

    interface Listener {
        /** Focus reached an option (for example to play a preview of it). */
        void onFocused(int index);

        void onChosen(int index);
    }

    private final List<TextView> chips = new ArrayList<>();
    private int selectedIndex = -1;

    /** Lets the owner restyle an option's text, e.g. to show a font in itself. */
    interface Decorator {
        void decorate(int index, TextView chip);
    }

    ChoiceGrid(Context context, @StringRes int[] labels, int selected, int columns, Listener listener) {
        this(context, labels, selected, columns, listener, null);
    }

    ChoiceGrid(Context context, @StringRes int[] labels, int selected, int columns, Listener listener,
               Decorator decorator) {
        super(context);
        TextStyler styler = TextStyler.create(context, AppSettings.from(context));
        selectedIndex = selected;
        int height = getResources().getDimensionPixelSize(R.dimen.chip_height);
        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        List<View> tiles = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            TextView chip = new TextView(context);
            chip.setText(labels[i]);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setTextColor(context.getColorStateList(R.color.row_text));
            chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.chip_text));
            chip.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            styler.style(chip);
            if (decorator != null) {
                decorator.decorate(i, chip);
            }
            chip.setBackgroundResource(R.drawable.setting_chip_background);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setActivated(i == selected);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height, 1f);
            params.setMargins(0, 0, gap, gap);
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> {
                setSelected(index);
                listener.onChosen(index);
            });
            chip.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    listener.onFocused(index);
                }
            });
            chips.add(chip);
            tiles.add(chip);
        }
        setTiles(tiles, columns, selected >= 0 && selected < chips.size() ? chips.get(selected) : null);
    }

    @Override
    public View preferredFocus() {
        return selectedIndex >= 0 && selectedIndex < chips.size() ? chips.get(selectedIndex) : super.preferredFocus();
    }

    void setSelected(int index) {
        selectedIndex = index;
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).setActivated(i == index);
        }
    }
}
