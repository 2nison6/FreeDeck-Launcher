package com.nison.freedeck.ui.map;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.nison.freedeck.R;

/**
 * A dotted world map that fills the view, with a pulsing pin at one location and an optional line
 * from the pin to a point elsewhere on the view (for example the edge of a card).
 * The map is scaled to cover the view, so on a screen that is not 16:9 its edges are cropped.
 */
public class WorldMapView extends View {

    private static final long PULSE_DURATION_MS = 2400L;
    private static final int PULSE_RING_COUNT = 2;
    private static final float PULSE_START_ALPHA = 0.6f;

    private final Drawable map;
    private final Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint endPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float pinRadius;
    private final float pulseRadius;
    private final float connectorEndRadius;
    private final ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0f, 1f);

    private float mapLeft;
    private float mapTop;
    private float mapWidth;
    private float mapHeight;
    private float pulsePhase;

    private boolean hasLocation;
    private double latitude;
    private double longitude;
    private boolean hasConnectorEnd;
    private float connectorEndX;
    private float connectorEndY;

    public WorldMapView(Context context) {
        this(context, null);
    }

    public WorldMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources resources = getResources();
        pinRadius = resources.getDimension(R.dimen.map_pin_radius);
        pulseRadius = resources.getDimension(R.dimen.map_pulse_radius);
        connectorEndRadius = resources.getDimension(R.dimen.map_connector_end_radius);

        map = context.getDrawable(R.drawable.world_map_dots).mutate();
        map.setAlpha(resources.getInteger(R.integer.map_dots_alpha));
        map.setTint(context.getColor(R.color.map_dots)); // the picture is white dots; this recolours them for the mode

        int pinColor = context.getColor(R.color.text_primary);
        pinPaint.setColor(pinColor);
        pulsePaint.setColor(pinColor);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(resources.getDimension(R.dimen.map_pulse_stroke));
        linePaint.setColor(context.getColor(R.color.map_connector));
        linePaint.setStrokeWidth(resources.getDimension(R.dimen.map_connector_width));
        endPaint.setColor(context.getColor(R.color.map_connector));

        pulseAnimator.setDuration(PULSE_DURATION_MS);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulsePhase = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        hasLocation = true;
        invalidate();
    }

    public void clearLocation() {
        hasLocation = false;
        invalidate();
    }

    /** Draws a line from the pin to this point, in this view's coordinates. */
    public void setConnectorEnd(float x, float y) {
        hasConnectorEnd = true;
        connectorEndX = x;
        connectorEndY = y;
        invalidate();
    }

    public void clearConnectorEnd() {
        hasConnectorEnd = false;
        invalidate();
    }

    /** The pin's position in this view's coordinates, or null while there is no location or no size yet. */
    @Nullable
    public PointF getPinPosition() {
        if (!hasLocation || mapWidth <= 0f) {
            return null;
        }
        return new PointF(
                mapLeft + MapProjection.xFraction(longitude) * mapWidth,
                mapTop + MapProjection.yFraction(latitude) * mapHeight);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        // Scale to cover the whole view, centred; any overflow is cropped by the view bounds.
        mapWidth = width;
        mapHeight = mapWidth / MapProjection.ASPECT_RATIO;
        if (mapHeight < height) {
            mapHeight = height;
            mapWidth = mapHeight * MapProjection.ASPECT_RATIO;
        }
        mapLeft = (width - mapWidth) / 2f;
        mapTop = (height - mapHeight) / 2f;
        map.setBounds(Math.round(mapLeft), Math.round(mapTop),
                Math.round(mapLeft + mapWidth), Math.round(mapTop + mapHeight));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        map.draw(canvas);
        PointF pin = getPinPosition();
        if (pin == null) {
            return;
        }
        if (hasConnectorEnd) {
            canvas.drawLine(pin.x, pin.y, connectorEndX, connectorEndY, linePaint);
            canvas.drawCircle(connectorEndX, connectorEndY, connectorEndRadius, endPaint);
        }
        for (int ring = 0; ring < PULSE_RING_COUNT; ring++) {
            float phase = (pulsePhase + (float) ring / PULSE_RING_COUNT) % 1f;
            pulsePaint.setAlpha(Math.round(255 * PULSE_START_ALPHA * (1f - phase)));
            canvas.drawCircle(pin.x, pin.y, pinRadius + phase * (pulseRadius - pinRadius), pulsePaint);
        }
        canvas.drawCircle(pin.x, pin.y, pinRadius, pinPaint);
    }

    /** Animates only while actually on screen, so a hidden page costs nothing. */
    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible) {
            if (!pulseAnimator.isStarted()) {
                pulseAnimator.start();
            }
        } else {
            pulseAnimator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        pulseAnimator.cancel();
        super.onDetachedFromWindow();
    }
}
