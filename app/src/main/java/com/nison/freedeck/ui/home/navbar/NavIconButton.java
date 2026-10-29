package com.nison.freedeck.ui.home.navbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/** Icon-only, D-pad focusable nav button. Grows slightly while focused. */
public class NavIconButton extends ImageView {

    private static final float FOCUSED_SCALE = 1.1f;
    private static final long ANIMATION_MS = 120L;

    public NavIconButton(Context context) {
        this(context, null);
    }

    public NavIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setClickable(true);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        float scale = gainFocus ? FOCUSED_SCALE : 1f;
        animate().scaleX(scale).scaleY(scale).setDuration(ANIMATION_MS).start();
    }
}
