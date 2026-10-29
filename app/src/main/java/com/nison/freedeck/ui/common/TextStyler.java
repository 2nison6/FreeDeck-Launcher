package com.nison.freedeck.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.FontStyle;

/**
 * Gives every piece of text the user's chosen font and colour as the screens are built. It sits in the layout
 * inflater, so text on every page and in every popup is covered without each one having to ask.
 */
public final class TextStyler implements LayoutInflater.Factory2 {

    private final Typeface typeface;
    private final boolean recolor;
    private final int primary;
    private final int secondary;
    private final int custom;

    private TextStyler(Typeface typeface, Integer customColor, Context context) {
        this.typeface = typeface;
        this.recolor = customColor != null;
        this.primary = context.getColor(R.color.text_primary);
        this.secondary = context.getColor(R.color.text_secondary);
        this.custom = customColor != null ? customColor : 0;
    }

    /** A styler for the user's current font and colour; it does nothing if neither was changed. */
    public static TextStyler create(Context context, AppSettings settings) {
        return new TextStyler(load(context, settings.fontStyle()), settings.fontColor(), context);
    }

    /** Starts styling everything {@code inflater} builds from now on. Does nothing if nothing is customised. */
    public static void install(LayoutInflater inflater, Context context, AppSettings settings) {
        TextStyler styler = create(context, settings);
        if (styler.typeface == null && !styler.recolor) {
            return;
        }
        try {
            inflater.setFactory2(styler);
        } catch (IllegalStateException alreadySet) {
            // Another factory got there first: leave the text as it is rather than fail.
        }
    }

    /** The typeface for a style, or null for the launcher's own look. */
    public static Typeface load(Context context, FontStyle style) {
        if (style.fontResource != 0) {
            try {
                return context.getResources().getFont(style.fontResource);
            } catch (RuntimeException e) {
                return null;
            }
        }
        return style.family != null ? Typeface.create(style.family, Typeface.NORMAL) : null;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        TextView view;
        if (name.equals("TextView")) {
            view = new TextView(context, attrs);
        } else if (name.equals("Button")) {
            view = new Button(context, attrs);
        } else {
            return null; // not plain text: let the inflater build it as usual
        }
        style(view);
        return view;
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

    /** Gives one piece of text the chosen font and colour. */
    public void style(TextView view) {
        if (typeface != null) {
            view.setTypeface(typeface);
        }
        if (recolor) {
            ColorStateList colors = view.getTextColors();
            int normal = TextRecolor.remap(colors.getDefaultColor(), primary, secondary, custom);
            if (colors.isStateful()) {
                int focused = colors.getColorForState(new int[]{android.R.attr.state_focused}, colors.getDefaultColor());
                view.setTextColor(new ColorStateList(
                        new int[][]{{android.R.attr.state_focused}, {}}, new int[]{focused, normal}));
            } else {
                view.setTextColor(normal);
            }
        }
    }
}
