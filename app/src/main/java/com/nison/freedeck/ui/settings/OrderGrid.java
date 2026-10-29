package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.ItemOrder;
import com.nison.freedeck.ui.common.TextStyler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Items the user can put in a different order with the remote. OK picks the focused item up, the arrow keys move
 * it (left / right one place, up / down a whole row), and OK or Back puts it down. Every move is reported at once
 * so whatever shows the order can follow it live.
 */
class OrderGrid<T> extends TileGrid {

    private final List<T> order;
    private final Map<T, TextView> chips = new HashMap<>();
    private final int columns;
    private final Consumer<List<T>> onChanged;
    private T grabbed;
    private boolean relayingOut;

    private final Predicate<T> shown;

    /** @param shown which items are listed; the others keep their place in the order but are not offered */
    OrderGrid(Context context, List<T> initial, int columns, Function<T, CharSequence> label,
              Predicate<T> shown, Consumer<List<T>> onChanged) {
        super(context);
        this.shown = shown;
        this.order = new ArrayList<>(initial);
        this.columns = columns;
        this.onChanged = onChanged;
        TextStyler styler = TextStyler.create(context, AppSettings.from(context));
        int height = getResources().getDimensionPixelSize(R.dimen.chip_height);
        int gap = getResources().getDimensionPixelSize(R.dimen.chip_gap);
        for (T item : initial) {
            TextView chip = new TextView(context);
            chip.setText(label.apply(item));
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setTextColor(context.getColorStateList(R.color.row_text));
            chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text));
            chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            styler.style(chip);
            chip.setBackgroundResource(R.drawable.setting_chip_background);
            chip.setFocusable(true);
            chip.setClickable(true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, height, 1f);
            params.setMargins(0, 0, gap, gap);
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> toggleGrab(item));
            chip.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && !relayingOut && grabbed == item) {
                    drop();
                }
            });
            chips.put(item, chip);
        }
        layout(null);
    }

    /** Lists the items again, e.g. after some were switched on or off elsewhere. */
    void refresh() {
        drop();
        layout(null);
    }

    private List<T> listed() {
        List<T> items = new ArrayList<>();
        for (T item : order) {
            if (shown.test(item)) {
                items.add(item);
            }
        }
        return items;
    }

    private void layout(T focusOn) {
        List<View> tiles = new ArrayList<>();
        List<T> listed = listed();
        for (T item : listed) {
            tiles.add(chips.get(item));
        }
        T first = focusOn != null ? focusOn : (listed.isEmpty() ? null : listed.get(0));
        setTiles(tiles, columns, first != null ? chips.get(first) : null);
    }

    private void toggleGrab(T item) {
        if (grabbed == item) {
            drop();
        } else {
            grabbed = item;
            chips.get(item).setActivated(true);
        }
    }

    private void drop() {
        if (grabbed != null) {
            chips.get(grabbed).setActivated(false);
            grabbed = null;
        }
    }

    @Override
    protected boolean onKey(View v, int keyCode, KeyEvent event) {
        if (grabbed == null || event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKey(v, keyCode, event);
        }
        List<T> listed = listed();
        int position = listed.indexOf(grabbed);
        int target;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                target = position - 1;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                target = position + 1;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                target = position - columns;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                target = position + columns;
                break;
            case KeyEvent.KEYCODE_BACK:
                drop();
                return true;
            default:
                return super.onKey(v, keyCode, event);
        }
        if (target >= 0 && target < listed.size()
                && ItemOrder.swap(order, order.indexOf(grabbed), order.indexOf(listed.get(target)))) {
            T item = grabbed;
            relayingOut = true; // rebuilding the rows briefly takes focus away; that is not a drop
            layout(item);
            chips.get(item).requestFocus();
            relayingOut = false;
            onChanged.accept(new ArrayList<>(order));
        }
        return true; // while holding an item the arrows only move it, they never leave the list
    }
}
