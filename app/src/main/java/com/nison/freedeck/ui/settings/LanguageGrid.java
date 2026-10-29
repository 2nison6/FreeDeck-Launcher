package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppLanguage;
import com.nison.freedeck.ui.common.TextStyler;
import com.nison.freedeck.data.settings.AppSettings;

import java.util.ArrayList;
import java.util.List;

/** The languages as chips, each written in its own script. Same behaviour as the other option lists. */
class LanguageGrid extends TileGrid {

    private static final int COLUMNS = 4;

    private final List<TextView> chips = new ArrayList<>();

    LanguageGrid(Context context, AppLanguage[] languages, int selected, ChoiceGrid.Listener listener) {
        super(context);
        int height = getResources().getDimensionPixelSize(R.dimen.chip_height);
        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        TextStyler styler = TextStyler.create(context, AppSettings.from(context));
        List<android.view.View> tiles = new ArrayList<>();
        for (int i = 0; i < languages.length; i++) {
            int index = i;
            TextView chip = new TextView(context);
            chip.setText(languages[i].nativeName);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setTextColor(context.getColorStateList(R.color.row_text));
            chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text));
            // Always the system font: the chosen display font may not have this script's letters.
            chip.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
            chip.setBackgroundResource(R.drawable.setting_chip_background);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setActivated(i == selected);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height, 1f);
            params.setMargins(0, 0, gap, gap);
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> listener.onChosen(index));
            chip.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    listener.onFocused(index);
                }
            });
            chips.add(chip);
            tiles.add(chip);
        }
        setTiles(tiles, COLUMNS, chips.get(selected));
    }
}
