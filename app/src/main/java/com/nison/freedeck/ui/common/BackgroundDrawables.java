package com.nison.freedeck.ui.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.nison.freedeck.R;
import androidx.annotation.Nullable;

import com.nison.freedeck.data.settings.AppBackground;
import com.nison.freedeck.data.settings.CustomBackground;

/** Paints a chosen {@link AppBackground} as the same linear gradient the glow behind the focused app uses. */
public final class BackgroundDrawables {

    /** How strong the colours are at the bottom-left corner and in the middle (0-255). */
    private static final int ALPHA_START = 150;
    private static final int ALPHA_MIDDLE = 90;

    private BackgroundDrawables() {
    }

    /** What to draw full screen: the scene's or uploaded picture, or the gradient. Null when there is nothing to draw. */
    @Nullable
    public static Drawable create(Context context, AppBackground background) {
        if (background.isNone()) {
            return null;
        }
        if (background.isCustom()) {
            return CustomBackground.drawable(context, background.slot());
        }
        return background.isScene() ? context.getDrawable(background.picture) : gradient(background);
    }

    /** The backgrounds a random mode cycles through: the built-in ones, or the pictures uploaded so far. */
    public static java.util.List<AppBackground> pool(Context context, com.nison.freedeck.data.settings.BackgroundRandom mode) {
        java.util.List<AppBackground> pool = new java.util.ArrayList<>();
        boolean custom = mode == com.nison.freedeck.data.settings.BackgroundRandom.CUSTOM;
        for (AppBackground background : AppBackground.values()) {
            if (background.isNone() || background.isCustom() != custom) {
                continue;
            }
            if (!custom || CustomBackground.exists(context, background.slot())) {
                pool.add(background);
            }
        }
        return pool;
    }

    /** See-through, so the screen's own colour (dark or light) shows through as the gradient fades out. */
    static GradientDrawable gradient(AppBackground background) {
        int first = withAlpha(background.colors[0], ALPHA_START);
        int second = withAlpha(background.colors[1], ALPHA_MIDDLE);
        return new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                new int[]{first, second, withAlpha(background.colors[1], 0)});
    }

    /** A small preview: the scene's thumbnail, or the gradient laid on the screen colour (nothing is behind it). */
    public static Drawable preview(Context context, AppBackground background) {
        if (background.isCustom()) {
            return CustomBackground.thumbnail(context, background.slot());
        }
        if (background.isScene()) {
            return context.getDrawable(background.thumbnail);
        }
        int screen = context.getColor(R.color.background);
        int[] colors = gradient(background).getColors();
        int[] blended = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            blended[i] = over(colors[i], screen);
        }
        return new GradientDrawable(GradientDrawable.Orientation.BL_TR, blended);
    }

    static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /** {@code top} (with its alpha) laid over the opaque colour {@code bottom}. */
    static int over(int top, int bottom) {
        int alpha = top >>> 24;
        int r = blend((top >> 16) & 0xFF, (bottom >> 16) & 0xFF, alpha);
        int g = blend((top >> 8) & 0xFF, (bottom >> 8) & 0xFF, alpha);
        int b = blend(top & 0xFF, bottom & 0xFF, alpha);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int blend(int top, int bottom, int alpha) {
        return (top * alpha + bottom * (255 - alpha) + 127) / 255;
    }
}
