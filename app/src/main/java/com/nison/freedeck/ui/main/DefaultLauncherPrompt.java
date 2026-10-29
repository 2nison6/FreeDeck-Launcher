package com.nison.freedeck.ui.main;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.system.SystemScreensaver;
import com.nison.freedeck.ui.common.TextStyler;

/**
 * A banner along the bottom of the screen, like a cookie notice: "Allow FreeDeck to be your Default Launcher" with Deny and Allow. Allow turns
 * the setting on (and the Home-button service with it). Deny is remembered, so the popup never comes back. Back just
 * closes it without deciding.
 */
class DefaultLauncherPrompt extends Dialog {

    private final Runnable onChanged;

    DefaultLauncherPrompt(Context context, Runnable onChanged) {
        super(context, R.style.OptionsDialog);
        this.onChanged = onChanged;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextStyler.install(getWindow().getLayoutInflater(), getContext(), AppSettings.from(getContext()));
        setContentView(R.layout.dialog_default_launcher);
        // Docked to the bottom edge, full width, with the screen behind dimmed (the dialog theme's dim amount).
        android.view.Window window = getWindow();
        window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(android.view.Gravity.BOTTOM);
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.7f);
        AppSettings settings = AppSettings.from(getContext());
        findViewById(R.id.default_launcher_deny).setOnClickListener(v -> {
            settings.setDefaultLauncherPromptDenied(true); // never asked again, even if the setting is off later
            dismiss();
        });
        findViewById(R.id.default_launcher_allow).setOnClickListener(v -> {
            settings.setDefaultLauncher(true);
            Context context = getContext();
            dismiss();
            AdbHelpDialog.enableOrExplain(context, () -> {
                // Being the home screen means using its screensaver too, not the TV's. That change needs the same
                // permission, so it waits until the permission has been sorted out.
                settings.setScreensaverOn(true);
                SystemScreensaver.followLauncher(context, true);
                onChanged.run();
            });
        });
        findViewById(R.id.default_launcher_allow).requestFocus();
    }
}
