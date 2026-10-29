package com.nison.freedeck.data.settings;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/**
 * What sits behind the Home and All Apps pages, or nothing. A <em>scene</em> is a full picture (mountains, a city
 * skyline and so on); a <em>gradient</em> is a soft colour like the glow behind the focused app, strongest in the
 * bottom-left corner and fading into the screen's own colour; <em>custom</em> is a picture the user uploaded.
 * Mountains is the default.
 */
public enum AppBackground {
    NONE("none", R.string.background_none, Kind.NONE, 0, 0),

    SCENE_MOUNTAINS("scene_mountains", R.string.background_mountains, Kind.SCENE, R.drawable.scene_mountains, R.drawable.scene_mountains_thumb),
    SCENE_NIGHT_SKY("scene_night_sky", R.string.background_night_sky, Kind.SCENE, R.drawable.scene_night_sky, R.drawable.scene_night_sky_thumb),
    SCENE_CITY("scene_city", R.string.background_city, Kind.SCENE, R.drawable.scene_city, R.drawable.scene_city_thumb),
    SCENE_SUNSET_SEA("scene_sunset_sea", R.string.background_sunset_sea, Kind.SCENE, R.drawable.scene_sunset_sea, R.drawable.scene_sunset_sea_thumb),
    SCENE_PINE_FOREST("scene_pine_forest", R.string.background_pine_forest, Kind.SCENE, R.drawable.scene_pine_forest, R.drawable.scene_pine_forest_thumb),
    SCENE_DESERT("scene_desert", R.string.background_desert, Kind.SCENE, R.drawable.scene_desert, R.drawable.scene_desert_thumb),
    SCENE_NORTHERN_LIGHTS("scene_northern_lights", R.string.background_northern_lights, Kind.SCENE, R.drawable.scene_northern_lights, R.drawable.scene_northern_lights_thumb),

    AURORA("aurora", R.string.background_aurora, Kind.GRADIENT, 0, 0, 0xFF00E5A8, 0xFF7C4DFF),
    SUNSET("sunset", R.string.background_sunset, Kind.GRADIENT, 0, 0, 0xFFFF6A3D, 0xFFD81B60),
    OCEAN("ocean", R.string.background_ocean, Kind.GRADIENT, 0, 0, 0xFF00B4D8, 0xFF0077B6),
    FOREST("forest", R.string.background_forest, Kind.GRADIENT, 0, 0, 0xFF66BB6A, 0xFF00695C),
    NEBULA("nebula", R.string.background_nebula, Kind.GRADIENT, 0, 0, 0xFFFF4081, 0xFF3D5AFE),
    EMBER("ember", R.string.background_ember, Kind.GRADIENT, 0, 0, 0xFFFFB300, 0xFFE53935),

    /** The pictures the user uploaded; their files are kept by {@code CustomBackground}. */
    CUSTOM_1("custom_1", R.string.background_custom_1, Kind.CUSTOM, 0, 0),
    CUSTOM_2("custom_2", R.string.background_custom_2, Kind.CUSTOM, 0, 0),
    CUSTOM_3("custom_3", R.string.background_custom_3, Kind.CUSTOM, 0, 0),
    CUSTOM_4("custom_4", R.string.background_custom_4, Kind.CUSTOM, 0, 0),
    CUSTOM_5("custom_5", R.string.background_custom_5, Kind.CUSTOM, 0, 0);

    /** What sort of background it is. */
    public enum Kind { NONE, SCENE, GRADIENT, CUSTOM }

    public static final AppBackground DEFAULT = SCENE_MOUNTAINS;

    /** Stable key for saving the choice. */
    public final String id;
    @StringRes
    public final int label;
    public final Kind kind;
    /** The full-screen picture of a scene; 0 otherwise. */
    @DrawableRes
    public final int picture;
    /** A small version of the picture for the picker; 0 otherwise. */
    @DrawableRes
    public final int thumbnail;
    /** The two colours of a gradient, from the bottom-left corner on; empty otherwise. */
    public final int[] colors;

    AppBackground(String id, @StringRes int label, Kind kind, @DrawableRes int picture, @DrawableRes int thumbnail,
                  int... colors) {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.picture = picture;
        this.thumbnail = thumbnail;
        this.colors = colors;
    }

    public boolean isNone() {
        return kind == Kind.NONE;
    }

    public boolean isScene() {
        return kind == Kind.SCENE;
    }

    public boolean isCustom() {
        return kind == Kind.CUSTOM;
    }

    /** Which upload a custom background is (1 to 5); 0 for the others. */
    public int slot() {
        return isCustom() ? Integer.parseInt(id.substring(id.length() - 1)) : 0;
    }

    /** Custom 1 to Custom 5. */
    public static AppBackground forSlot(int slot) {
        for (AppBackground background : values()) {
            if (background.isCustom() && background.slot() == slot) {
                return background;
            }
        }
        return DEFAULT;
    }

    /** The background saved under {@code id}; the default when there is none or it is not recognised. */
    public static AppBackground fromId(String id) {
        if ("custom".equals(id)) {
            return CUSTOM_1; // the single upload from before there were five
        }
        for (AppBackground background : values()) {
            if (background.id.equals(id)) {
                return background;
            }
        }
        return DEFAULT;
    }
}
