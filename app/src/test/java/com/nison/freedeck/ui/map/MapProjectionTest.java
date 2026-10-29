package com.nison.freedeck.ui.map;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Expected values were computed independently from the Mercator formulas in tools/generate_world_map.py. */
public class MapProjectionTest {

    private static final float DELTA = 0.001f;

    @Test
    public void bengaluru() {
        assertEquals(0.7155f, MapProjection.xFraction(77.59), DELTA);
        assertEquals(0.6001f, MapProjection.yFraction(12.97), DELTA);
    }

    @Test
    public void london() {
        assertEquals(0.4996f, MapProjection.xFraction(-0.13), DELTA);
        assertEquals(0.3669f, MapProjection.yFraction(51.51), DELTA);
    }

    @Test
    public void sydney() {
        assertEquals(0.9200f, MapProjection.xFraction(151.21), DELTA);
        assertEquals(0.8426f, MapProjection.yFraction(-33.87), DELTA);
    }

    @Test
    public void westernAndEasternEdges() {
        assertEquals(0f, MapProjection.xFraction(-180), DELTA);
        assertEquals(1f, MapProjection.xFraction(180), DELTA);
    }

    @Test
    public void primeMeridianIsHorizontalCentre() {
        assertEquals(0.5f, MapProjection.xFraction(0), DELTA);
    }

    @Test
    public void bottomLatitudeIsTheBottomEdge() {
        assertEquals(1f, MapProjection.yFraction(-56), DELTA);
    }

    @Test
    public void topLatitudeIsTheTopEdge() {
        assertEquals(0f, MapProjection.yFraction(79.0962), DELTA);
    }

    @Test
    public void positionsOutsideTheMapAreClamped() {
        assertEquals(0f, MapProjection.yFraction(89), DELTA);
        assertEquals(1f, MapProjection.yFraction(-89), DELTA);
        assertEquals(1f, MapProjection.xFraction(200), DELTA);
    }

    @Test
    public void northIsAboveSouth() {
        assertEquals(true, MapProjection.yFraction(40) < MapProjection.yFraction(-20));
    }

    @Test
    public void imageIsSixteenByNine() {
        assertEquals(16f / 9f, MapProjection.ASPECT_RATIO, DELTA);
    }
}
