package com.nison.freedeck.ui.apps;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.InstalledApp;

import java.util.Date;

/** An app's summary and what can be done with it, shown on a long press. */
public class AppOptionsDialog extends Dialog {

    private final InstalledApp app;
    private final boolean favorite;
    private final Runnable onToggleFavorite;

    public AppOptionsDialog(Context context, InstalledApp app, boolean favorite, Runnable onToggleFavorite) {
        super(context, R.style.OptionsDialog);
        this.app = app;
        this.favorite = favorite;
        this.onToggleFavorite = onToggleFavorite;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_options);

        bindImage();
        ((TextView) findViewById(R.id.options_title)).setText(app.label);
        bindVersion();
        ((TextView) findViewById(R.id.options_body)).setText(bodyText());

        Button toggle = findViewById(R.id.options_favorite);
        toggle.setText(favorite ? R.string.app_favorite_remove : R.string.app_favorite_add);
        toggle.setOnClickListener(v -> {
            dismiss();
            onToggleFavorite.run();
        });
        toggle.requestFocus();
    }

    private void bindImage() {
        ImageView image = findViewById(R.id.options_icon);
        image.setImageDrawable(app.cardImage);
        image.setScaleType(app.hasBanner ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        int padding = app.hasBanner ? 0 : getContext().getResources().getDimensionPixelSize(R.dimen.app_icon_padding);
        image.setPadding(padding, padding, padding, padding);
    }

    private void bindVersion() {
        TextView view = findViewById(R.id.options_version);
        view.setText(app.versionName != null ? getContext().getString(R.string.app_version, app.versionName) : "");
        view.setVisibility(app.versionName != null ? View.VISIBLE : View.GONE);
    }

    /** The app's own description when it has one, otherwise what the system knows: when it was installed, and its id. */
    private String bodyText() {
        if (app.description != null) {
            return app.description;
        }
        String installed = DateFormat.getMediumDateFormat(getContext()).format(new Date(app.installTimeMillis));
        return getContext().getString(R.string.app_details_fallback, installed, app.packageName);
    }
}
