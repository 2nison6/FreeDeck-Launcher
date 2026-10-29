package com.nison.freedeck.ui.home.pages;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.AppCatalog;
import com.nison.freedeck.ui.apps.AppGridView;
import com.nison.freedeck.ui.common.GlowView;
import com.nison.freedeck.ui.sound.FocusSound;

/** The home page: the favorite apps along the bottom of the screen. */
public class HomePage extends FrameLayout {

    private final AppGridView favorites;
    private final GlowView glow;

    public HomePage(Context context) {
        this(context, null);
    }

    public HomePage(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.page_home, this, true);
        favorites = findViewById(R.id.home_grid);
        glow = findViewById(R.id.home_glow);
    }

    public void bind(AppCatalog catalog, FocusSound focusSound) {
        favorites.bind(catalog, AppGridView.Source.FAVORITES, R.id.nav_home, glow, focusSound);
    }
}
