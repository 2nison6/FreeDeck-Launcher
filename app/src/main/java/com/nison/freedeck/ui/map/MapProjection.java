package com.nison.freedeck.ui.map;

/**
 * Maps latitude/longitude onto the world map image: a Mercator projection from 56°S up to whatever
 * latitude makes the image 16:9, so it fills a TV screen. Must match tools/generate_world_map.py.
 */
public final class MapProjection {

    private static final double ASPECT = 1920.0 / 1080.0;

    /** Width divided by height of the map image. */
    public static final float ASPECT_RATIO = (float) ASPECT;

    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;
    private static final double LAT_BOTTOM = -56.0;
    /** Mercator stretches to infinity at the poles, so latitudes are limited before projecting. */
    private static final double MAX_LATITUDE = 85.0;
    private static final double Y_BOTTOM = mercatorY(LAT_BOTTOM);
    private static final double Y_TOP = Y_BOTTOM + 2 * Math.PI / ASPECT;

    private MapProjection() {
    }

    /** Horizontal position as a fraction (0 = left edge, 1 = right edge) of the map width. */
    public static float xFraction(double longitude) {
        return clamp((longitude - LON_MIN) / (LON_MAX - LON_MIN));
    }

    /** Vertical position as a fraction (0 = top edge, 1 = bottom edge) of the map height. */
    public static float yFraction(double latitude) {
        return clamp((Y_TOP - mercatorY(latitude)) / (Y_TOP - Y_BOTTOM));
    }

    private static double mercatorY(double latitude) {
        double limited = Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, latitude));
        return Math.log(Math.tan(Math.PI / 4 + Math.toRadians(limited) / 2));
    }

    private static float clamp(double fraction) {
        return (float) Math.max(0.0, Math.min(1.0, fraction));
    }
}
