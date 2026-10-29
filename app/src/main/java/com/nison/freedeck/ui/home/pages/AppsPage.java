package com.nison.freedeck.ui.home.pages;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.AppCatalog;
import com.nison.freedeck.data.settings.AppLayout;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.ui.apps.AppGridView;
import com.nison.freedeck.ui.common.GlowView;
import com.nison.freedeck.ui.sound.FocusSound;

/** Every user-installed app, favorites first. */
public class AppsPage extends FrameLayout {

    private final AppGridView grid;
    private final GlowView glow;

    public AppsPage(Context context) {
        this(context, null);
    }

    public AppsPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.page_apps, this, true);
        grid = findViewById(R.id.apps_grid);
        glow = findViewById(R.id.apps_glow);
        TextView emptyMessage = findViewById(R.id.apps_empty);
        grid.setOnItemCountChangedListener(count -> emptyMessage.setVisibility(count == 0 ? View.VISIBLE : View.GONE));
    }

    public void bind(AppCatalog catalog, AppSettings settings, FocusSound focusSound) {
        grid.bind(catalog, AppGridView.Source.ALL, R.id.nav_apps, glow, focusSound);
        applyLayout(settings);
        settings.addAppLayoutListener(() -> applyLayout(settings));
    }

    private void applyLayout(AppSettings settings) {
        AppLayout layout = settings.appLayout();
        grid.setGrid(layout.columns, layout.rows);
    }
}
