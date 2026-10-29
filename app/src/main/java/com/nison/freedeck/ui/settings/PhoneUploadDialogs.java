package com.nison.freedeck.ui.settings;

import android.content.Context;

/** Entry point for the "add a picture from your phone" popup. */
public final class PhoneUploadDialogs {

    private PhoneUploadDialogs() {
    }

    /**
     * @param slot which of the five custom backgrounds the picture becomes (1 to 5)
     * @param onSaved called after a picture was received and kept
     */
    public static void show(Context context, int slot, Runnable onSaved) {
        new PhoneUploadDialog(context, slot, onSaved).show();
    }
}
