package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nison.freedeck.ui.apps.GridNavigation;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiles laid out in rows of a fixed number of columns, with the arrow keys handled here rather than by Android's
 * focus search: left and right read through the tiles (wrapping), down and up move a row, and moving down from the
 * last row or up from the first leaves to the neighbouring settings.
 */
abstract class TileGrid extends LinearLayout implements DropdownContent {

    @Override
    public View view() {
        return this;
    }

    private final List<View> tiles = new ArrayList<>();
    private int columns = 1;
    private View upExit;
    private View downExit;
    private View preferred;

    TileGrid(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    @Override
    public void setExits(View up, View down) {
        upExit = up;
        downExit = down;
    }

    /** Replaces the tiles. {@code preferredTile} may be null, or a tile in {@code newTiles}. */
    protected void setTiles(List<View> newTiles, int columnCount, View preferredTile) {
        // Keep the exits across a rebuild.

        // Tiles may still sit in the rows of the previous layout: take them out before they are placed again.
        for (View tile : newTiles) {
            if (tile.getParent() instanceof ViewGroup) {
                ((ViewGroup) tile.getParent()).removeView(tile);
            }
        }
        removeAllViews();
        tiles.clear();
        tiles.addAll(newTiles);
        columns = columnCount;
        preferred = preferredTile;
        LinearLayout row = null;
        for (View tile : tiles) {
            if (row == null || row.getChildCount() == columns) {
                row = new LinearLayout(getContext());
                row.setOrientation(HORIZONTAL);
                addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }
            // Keep the tile's own height and margins; only the width is shared out evenly across the row.
            ViewGroup.LayoutParams own = tile.getLayoutParams();
            LayoutParams params = new LayoutParams(0, own != null ? own.height : LayoutParams.WRAP_CONTENT, 1f);
            if (own instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) own;
                params.setMargins(margins.leftMargin, margins.topMargin, margins.rightMargin, margins.bottomMargin);
            }
            row.addView(tile, params);
            tile.setOnKeyListener(this::onKey);
        }
        // Empty slots in the last row keep every tile the same width as the ones above.
        while (row != null && row.getChildCount() < columns) {
            View spacer = new View(getContext());
            row.addView(spacer, new LayoutParams(0, 1, 1f));
        }
    }

    protected List<View> tiles() {
        return tiles;
    }

    @Override
    public View preferredFocus() {
        if (preferred != null) {
            return preferred;
        }
        return tiles.isEmpty() ? this : tiles.get(0);
    }

    protected boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        int position = tiles.indexOf(v);
        int count = tiles.size();
        if (position < 0) {
            return false;
        }
        View target;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                target = tiles.get(GridNavigation.afterRight(position, count));
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                target = tiles.get(GridNavigation.afterLeft(position, count));
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                target = position / columns < (count - 1) / columns
                        ? tiles.get(Math.min(position + columns, count - 1))
                        : downExit;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                target = position >= columns ? tiles.get(position - columns) : upExit;
                break;
            default:
                return false;
        }
        if (target != null) {
            target.requestFocus();
        }
        return true; // never left to Android's focus search, which could jump out of the settings
    }
}
