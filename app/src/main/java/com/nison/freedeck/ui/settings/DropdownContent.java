package com.nison.freedeck.ui.settings;

import android.view.View;

/** What opens under a setting when it has focus. */
public interface DropdownContent {

    View view();

    /** The view that takes focus when the user moves down into the dropdown (the chosen option, say). */
    View preferredFocus();

    /** Where focus goes when moving up out of the first row and down out of the last row. */
    void setExits(View up, View down);
}
