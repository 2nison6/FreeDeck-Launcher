package com.nison.freedeck.data.apps;

/** Picks the most prominent vivid colour from an image's pixels, for use as a glow behind the app. */
public final class DominantColor {

    private static final int BUCKET_COUNT = 12;
    private static final float BUCKET_DEGREES = 360f / BUCKET_COUNT;
    private static final int MIN_ALPHA = 128;
    /** Greys, whites and near-blacks carry no usable hue and are ignored. */
    private static final float MIN_SATURATION = 0.35f;
    private static final float MIN_VALUE = 0.35f;
    private static final float MIN_RESULT_SATURATION = 0.6f;

    private DominantColor() {
    }

    /** @return an opaque ARGB colour, or {@code fallback} when no pixel is colourful enough. */
    public static int fromPixels(int[] argb, int fallback) {
        float[] weight = new float[BUCKET_COUNT];
        float[] hueOffsetSum = new float[BUCKET_COUNT];
        float[] saturationSum = new float[BUCKET_COUNT];

        for (int pixel : argb) {
            if ((pixel >>> 24) < MIN_ALPHA) {
                continue;
            }
            float[] hsv = toHsv((pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF, pixel & 0xFF);
            if (hsv[1] < MIN_SATURATION || hsv[2] < MIN_VALUE) {
                continue;
            }
            // Buckets are centred on multiples of 30°, so reds on either side of 0° share one bucket.
            int bucket = (int) (((hsv[0] + BUCKET_DEGREES / 2) % 360f) / BUCKET_DEGREES);
            float offset = wrapDegrees(hsv[0] - bucket * BUCKET_DEGREES);
            float w = hsv[1] * hsv[2];
            weight[bucket] += w;
            hueOffsetSum[bucket] += offset * w;
            saturationSum[bucket] += hsv[1] * w;
        }

        int best = -1;
        for (int i = 0; i < BUCKET_COUNT; i++) {
            if (weight[i] > 0f && (best < 0 || weight[i] > weight[best])) {
                best = i;
            }
        }
        if (best < 0) {
            return fallback;
        }
        float hue = (best * BUCKET_DEGREES + hueOffsetSum[best] / weight[best] + 360f) % 360f;
        float saturation = Math.max(saturationSum[best] / weight[best], MIN_RESULT_SATURATION);
        return fromHsv(hue, saturation, 1f);
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360f;
        if (wrapped > 180f) {
            wrapped -= 360f;
        } else if (wrapped < -180f) {
            wrapped += 360f;
        }
        return wrapped;
    }

    private static float[] toHsv(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0f) {
            hue = 0f;
        } else if (max == r) {
            hue = 60f * (((g - b) / delta + 6f) % 6f);
        } else if (max == g) {
            hue = 60f * ((b - r) / delta + 2f);
        } else {
            hue = 60f * ((r - g) / delta + 4f);
        }
        return new float[]{hue, max == 0 ? 0f : delta / max, max / 255f};
    }

    private static int fromHsv(float hue, float saturation, float value) {
        float c = value * saturation;
        float x = c * (1f - Math.abs((hue / 60f) % 2f - 1f));
        float m = value - c;
        float r;
        float g;
        float b;
        switch ((int) (hue / 60f) % 6) {
            case 0:
                r = c; g = x; b = 0;
                break;
            case 1:
                r = x; g = c; b = 0;
                break;
            case 2:
                r = 0; g = c; b = x;
                break;
            case 3:
                r = 0; g = x; b = c;
                break;
            case 4:
                r = x; g = 0; b = c;
                break;
            default:
                r = c; g = 0; b = x;
                break;
        }
        return 0xFF000000
                | (Math.round((r + m) * 255f) << 16)
                | (Math.round((g + m) * 255f) << 8)
                | Math.round((b + m) * 255f);
    }
}
