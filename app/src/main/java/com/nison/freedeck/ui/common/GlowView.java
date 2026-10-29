package com.nison.freedeck.ui.common;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.nison.freedeck.R;

/**
 * A soft linear light in one colour: strongest in the bottom-left corner, fading to nothing toward the
 * top-right. Invisible until {@link #show} is called; changes fade rather than jump.
 */
public class GlowView extends View {

    private static final long FADE_MS = 400L;

    private final GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BL_TR, new int[2]);
    private final ArgbEvaluator evaluator = new ArgbEvaluator();
    private final int strongestAlpha;
    private ValueAnimator fade;
    /** The colour at the strong corner, including its alpha; alpha 0 means no glow. */
    private int current;

    public GlowView(Context context) {
        this(context, null);
    }

    public GlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        strongestAlpha = getResources().getInteger(R.integer.glow_start_alpha);
        setBackground(gradient);
        render(0);
    }

    /** Glows in {@code rgb}, fading in from nothing or across from the previous colour. */
    public void show(int rgb, boolean animate) {
        int opaque = rgb & 0x00FFFFFF;
        int from = (current >>> 24) == 0 ? opaque : current; // from "nothing" fade only the alpha, not the hue
        transition(from, (strongestAlpha << 24) | opaque, animate);
    }

    /** Fades the glow out. */
    public void hide(boolean animate) {
        transition(current, current & 0x00FFFFFF, animate);
    }

    private void transition(int from, int target, boolean animate) {
        if (fade != null) {
            fade.cancel();
        }
        if (!animate) {
            render(target);
            return;
        }
        fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(FADE_MS);
        fade.addUpdateListener(animation ->
                render((Integer) evaluator.evaluate(animation.getAnimatedFraction(), from, target)));
        fade.start();
    }

    private void render(int argb) {
        current = argb;
        gradient.setColors(new int[]{argb, argb & 0x00FFFFFF});
    }

    @Override
    protected void onDetachedFromWindow() {
        if (fade != null) {
            fade.cancel();
        }
        super.onDetachedFromWindow();
    }
}
