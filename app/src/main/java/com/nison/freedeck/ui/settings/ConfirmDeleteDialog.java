package com.nison.freedeck.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.ui.common.TextStyler;

/** A bar along the bottom asking "Delete Custom 2?", with Cancel (the one focused first) and Delete. */
class ConfirmDeleteDialog extends Dialog {

    private final CharSequence name;
    private final Runnable onConfirmed;

    ConfirmDeleteDialog(Context context, CharSequence name, Runnable onConfirmed) {
        super(context, R.style.OptionsDialog);
        this.name = name;
        this.onConfirmed = onConfirmed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextStyler.install(getWindow().getLayoutInflater(), getContext(), AppSettings.from(getContext()));
        setContentView(R.layout.dialog_confirm);
        Window window = getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.7f);
        ((TextView) findViewById(R.id.confirm_title)).setText(getContext().getString(R.string.confirm_delete_title, name));
        findViewById(R.id.confirm_cancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.confirm_ok).setOnClickListener(v -> {
            dismiss();
            onConfirmed.run();
        });
        findViewById(R.id.confirm_cancel).requestFocus();
    }
}
