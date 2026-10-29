package com.nison.freedeck.data.settings;

/** The arithmetic for shrinking a picture the user picked to the size of the screen. */
public final class ImageScale {

    private ImageScale() {
    }

    /** The power-of-two subsampling that keeps the picture at least {@code targetWidth} x {@code targetHeight}. */
    public static int sampleSize(int width, int height, int targetWidth, int targetHeight) {
        int sample = 1;
        while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) {
            sample *= 2;
        }
        return sample;
    }

    /** {@code width} x {@code height} scaled down, keeping its shape, to fit inside the target; never enlarged. */
    public static int[] fit(int width, int height, int maxWidth, int maxHeight) {
        double scale = Math.min(1.0, Math.min((double) maxWidth / width, (double) maxHeight / height));
        return new int[]{Math.max(1, (int) Math.round(width * scale)), Math.max(1, (int) Math.round(height * scale))};
    }
}
