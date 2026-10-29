package com.nison.freedeck.data.settings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ImageScaleTest {

    @Test
    public void aSmallPictureIsNotSubsampled() {
        assertEquals(1, ImageScale.sampleSize(1280, 720, 1920, 1080));
        assertEquals(1, ImageScale.sampleSize(1920, 1080, 1920, 1080));
    }

    @Test
    public void aHugePictureIsSubsampledButKeepsEnoughDetail() {
        assertEquals(2, ImageScale.sampleSize(4000, 3000, 1920, 1080));
        assertEquals(4, ImageScale.sampleSize(8000, 6000, 1920, 1080));
    }

    @Test
    public void subsamplingNeverMakesAnEdgeSmallerThanTheScreen() {
        // 3840x1100 halved is 1920x550, too short; so it must stay whole.
        assertEquals(1, ImageScale.sampleSize(3840, 1100, 1920, 1080));
    }

    @Test
    public void fitKeepsTheShapeAndNeverEnlarges() {
        assertArrayEquals(new int[]{1920, 1080}, ImageScale.fit(3840, 2160, 1920, 1080));
        assertArrayEquals(new int[]{1440, 1080}, ImageScale.fit(4000, 3000, 1920, 1080));
        assertArrayEquals(new int[]{800, 600}, ImageScale.fit(800, 600, 1920, 1080));
    }
}
