package com.nison.freedeck.data.settings;

import androidx.annotation.FontRes;
import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** The typefaces the launcher can use for its text. */
public enum FontStyle {
    SYSTEM("system", R.string.font_system, 0, null),
    SERIF("serif", R.string.font_serif, 0, "serif"),
    MONOSPACE("monospace", R.string.font_monospace, 0, "monospace"),
    POPPINS("poppins", R.string.font_poppins, R.font.poppins, null),
    MONTSERRAT("montserrat", R.string.font_montserrat, R.font.montserrat, null),
    OSWALD("oswald", R.string.font_oswald, R.font.oswald, null),
    BEBAS_NEUE("bebas_neue", R.string.font_bebas_neue, R.font.bebas_neue, null),
    ORBITRON("orbitron", R.string.font_orbitron, R.font.orbitron, null),
    RIGHTEOUS("righteous", R.string.font_righteous, R.font.righteous, null),
    COMFORTAA("comfortaa", R.string.font_comfortaa, R.font.comfortaa, null),
    FREDOKA("fredoka", R.string.font_fredoka, R.font.fredoka, null),
    PLAYFAIR("playfair", R.string.font_playfair, R.font.playfair_display, null),
    ABRIL_FATFACE("abril_fatface", R.string.font_abril_fatface, R.font.abril_fatface, null),
    LOBSTER("lobster", R.string.font_lobster, R.font.lobster, null),
    PACIFICO("pacifico", R.string.font_pacifico, R.font.pacifico, null),
    CAVEAT("caveat", R.string.font_caveat, R.font.caveat, null);

    /** The launcher's own look, left untouched. */
    public static final FontStyle DEFAULT = SYSTEM;

    public final String id;
    @StringRes
    public final int label;
    /** A bundled font file, or 0. */
    @FontRes
    public final int fontResource;
    /** A built-in Android family name (used when there is no bundled file), or null. */
    public final String family;

    FontStyle(String id, @StringRes int label, @FontRes int fontResource, String family) {
        this.id = id;
        this.label = label;
        this.fontResource = fontResource;
        this.family = family;
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public static FontStyle fromId(String id) {
        for (FontStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return DEFAULT;
    }
}
