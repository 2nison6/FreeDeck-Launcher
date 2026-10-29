package com.nison.freedeck.data.settings;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** How many app cards the All Apps page shows at once: columns across, rows down. The first is the default. */
public enum AppLayout {
    COLUMNS_5_ROWS_3("5x3", 5, 3, R.string.layout_5x3),
    COLUMNS_6_ROWS_3("6x3", 6, 3, R.string.layout_6x3),
    COLUMNS_3_ROWS_3("3x3", 3, 3, R.string.layout_3x3);

    public static final AppLayout DEFAULT = COLUMNS_5_ROWS_3;

    /** Stable key for saving the choice. */
    public final String id;
    public final int columns;
    /** Rows visible at once; more apps than fit scroll a row at a time. */
    public final int rows;
    @StringRes
    public final int label;

    AppLayout(String id, int columns, int rows, @StringRes int label) {
        this.id = id;
        this.columns = columns;
        this.rows = rows;
        this.label = label;
    }

    /** The layout saved under {@code id}; the default when there is none or it is not recognised. */
    public static AppLayout fromId(String id) {
        for (AppLayout layout : values()) {
            if (layout.id.equals(id)) {
                return layout;
            }
        }
        return DEFAULT;
    }
}
