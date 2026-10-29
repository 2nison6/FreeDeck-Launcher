package com.nison.freedeck.ui.common;

/** Works out which text colours are swapped for the user's chosen one. Pure, so it can be tested. */
public final class TextRecolor {

    private TextRecolor() {
    }

    /**
     * @param color     a colour a piece of text currently has
     * @param primary   the theme's main text colour
     * @param secondary the theme's dimmer text colour
     * @param custom    the colour the user chose
     * @return {@code custom} where {@code color} is the main text colour, {@code custom} at the dimmer colour's
     * transparency where it is the dimmer one, and {@code color} unchanged for anything else (such as the dark
     * text on a highlighted row)
     */
    public static int remap(int color, int primary, int secondary, int custom) {
        if (color == primary) {
            return custom;
        }
        if (color == secondary) {
            return (secondary & 0xFF000000) | (custom & 0x00FFFFFF);
        }
        return color;
    }
}
