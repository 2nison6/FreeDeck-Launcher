package com.nison.freedeck.ui.main;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.system.DefaultLauncher;
import com.nison.freedeck.ui.common.TextStyler;

/**
 * Shown when the app could not switch its Home-button service on by itself: says why, and gives the one command that
 * does it from a computer. Fire TV has no settings screen the user could use instead.
 */
public class AdbHelpDialog extends Dialog {

    private final int reason;

    private AdbHelpDialog(Context context, @StringRes int reason) {
        super(context, R.style.OptionsDialog);
        this.reason = reason;
    }

    /**
     * Switches the Home-button service on, asking the TV for the permission it needs if the app does not have it yet,
     * and explains on screen what to do when that fails. {@code onDone} runs afterwards either way.
     */
    public static void enableOrExplain(Context context, Runnable onDone) {
        if (!DefaultLauncher.canEnableByItself(context) && !DefaultLauncher.isServiceEnabled(context)) {
            android.widget.Toast.makeText(context, R.string.default_launcher_asking, android.widget.Toast.LENGTH_LONG).show();
        }
        DefaultLauncher.enable(context, outcome -> {
            if (outcome != DefaultLauncher.Outcome.ENABLED) {
                show(context, outcome);
            }
            onDone.run();
        });
    }

    /** Tells the user what happened and what to do, for the given result of trying to switch the service on. */
    public static void show(Context context, DefaultLauncher.Outcome outcome) {
        int reason = outcome == DefaultLauncher.Outcome.DEBUGGING_OFF ? R.string.adb_help_debugging_off
                : outcome == DefaultLauncher.Outcome.NOT_ALLOWED ? R.string.adb_help_not_allowed
                : R.string.adb_help_failed;
        new AdbHelpDialog(context, reason).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextStyler.install(getWindow().getLayoutInflater(), getContext(), AppSettings.from(getContext()));
        setContentView(R.layout.dialog_adb_help);
        Window window = getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.7f);
        ((TextView) findViewById(R.id.adb_help_reason)).setText(reason);
        ((TextView) findViewById(R.id.adb_help_command)).setText(
                getContext().getString(R.string.adb_help_command, DefaultLauncher.grantCommand(getContext())));
        findViewById(R.id.adb_help_ok).setOnClickListener(v -> dismiss());
        findViewById(R.id.adb_help_ok).requestFocus();
    }
}
