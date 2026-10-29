package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.ThemeMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the text colour for one mode with three bars (colour, richness, lightness), a swatch that shows the
 * result as "Aa" on that mode's own background, and Apply / Default buttons. Nothing is saved until Apply, so the
 * screen is not rebuilt on every step.
 */
class ModeColorPicker extends LinearLayout {

    private final ThemeMode mode;
    private final AppSettings settings;
    private final Runnable onApplied;
    private final TextView swatch;
    private final ColorSlider hue;
    private final ColorSlider saturation;
    private final ColorSlider lightness;
    private final TextView apply;
    private final TextView reset;
    private final float[] hsv = new float[3];

    ModeColorPicker(Context context, ThemeMode mode, @StringRes int title, AppSettings settings, Runnable onApplied) {
        super(context);
        this.mode = mode;
        this.settings = settings;
        this.onApplied = onApplied;
        setOrientation(VERTICAL);
        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        int height = getResources().getDimensionPixelSize(R.dimen.chip_height);

        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextColor(context.getColor(R.color.text_secondary));
        heading.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text));
        heading.setPadding(0, gap, 0, gap);
        addView(heading);

        swatch = new TextView(context);
        swatch.setText("Aa  The quick brown fox");
        swatch.setGravity(Gravity.CENTER);
        swatch.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text) * 1.3f);
        LayoutParams swatchParams = new LayoutParams(LayoutParams.MATCH_PARENT, height * 3 / 2);
        swatchParams.bottomMargin = gap;
        addView(swatch, swatchParams);

        hue = new ColorSlider(context, hueColors());
        saturation = new ColorSlider(context, new int[]{Color.WHITE, Color.RED});
        lightness = new ColorSlider(context, new int[]{Color.BLACK, Color.WHITE});
        for (ColorSlider slider : new ColorSlider[]{hue, saturation, lightness}) {
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, height * 2 / 3);
            params.bottomMargin = gap;
            addView(slider, params);
            slider.setListener(this::onSliderMoved);
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(HORIZONTAL);
        apply = button(context, R.string.color_apply, height, gap);
        reset = button(context, R.string.color_reset, height, gap);
        buttons.addView(apply);
        buttons.addView(reset);
        addView(buttons);

        apply.setOnClickListener(v -> {
            settings.setFontColor(mode, currentColor());
            onApplied.run();
        });
        reset.setOnClickListener(v -> {
            settings.setFontColor(mode, null);
            onApplied.run();
        });
        loadFrom(settings.fontColor(mode));
    }

    private static TextView button(Context context, int text, int height, int gap) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setTextColor(context.getColorStateList(R.color.row_text));
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.chip_text));
        view.setBackgroundResource(R.drawable.setting_chip_background);
        view.setFocusable(true);
        view.setClickable(true);
        LayoutParams params = new LayoutParams(0, height, 1f);
        params.setMargins(0, 0, gap, gap);
        view.setLayoutParams(params);
        return view;
    }

    private static int[] hueColors() {
        int[] colors = new int[7];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.HSVToColor(new float[]{i * 60f, 1f, 1f});
        }
        return colors;
    }

    /** Starts from the saved colour, or from plain white / near-black (the launcher's own) when there is none. */
    private void loadFrom(Integer color) {
        int start = color != null ? color : (mode == ThemeMode.DARK ? Color.WHITE : 0xFF111114);
        Color.colorToHSV(start, hsv);
        hue.setValue(hsv[0] / 360f);
        saturation.setValue(hsv[1]);
        lightness.setValue(hsv[2]);
        refresh();
    }

    private void onSliderMoved() {
        hsv[0] = hue.value() * 360f;
        hsv[1] = saturation.value();
        hsv[2] = lightness.value();
        refresh();
    }

    private int currentColor() {
        return Color.HSVToColor(new float[]{hue.value() * 360f, saturation.value(), lightness.value()});
    }

    private void refresh() {
        float h = hue.value() * 360f;
        saturation.setColors(new int[]{Color.HSVToColor(new float[]{h, 0f, lightness.value()}),
                Color.HSVToColor(new float[]{h, 1f, lightness.value()})});
        lightness.setColors(new int[]{Color.BLACK, Color.HSVToColor(new float[]{h, saturation.value(), 1f})});
        swatch.setTextColor(currentColor());
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(getResources().getDimension(R.dimen.settings_row_radius));
        // The swatch sits on this mode's own background, so the colour is judged as it will really be seen.
        background.setColor(mode == ThemeMode.DARK ? 0xFF000000 : 0xFFF2F2F7);
        swatch.setBackground(background);
    }

    /** The views that take focus, top to bottom: the three bars, then the buttons (left, right). */
    List<List<View>> focusRows() {
        List<List<View>> rows = new ArrayList<>();
        rows.add(single(hue));
        rows.add(single(saturation));
        rows.add(single(lightness));
        List<View> buttons = new ArrayList<>();
        buttons.add(apply);
        buttons.add(reset);
        rows.add(buttons);
        return rows;
    }

    private static List<View> single(View view) {
        List<View> row = new ArrayList<>();
        row.add(view);
        return row;
    }
}
