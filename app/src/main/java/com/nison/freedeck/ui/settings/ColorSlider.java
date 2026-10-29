package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.KeyEvent;
import android.view.View;

import com.nison.freedeck.R;

/** A coloured bar with a marker. Left and right on the remote move the marker; focus shows an outline. */
class ColorSlider extends View {

    interface Listener {
        void onValueChanged();
    }

    private static final int STEPS = 50;

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final int[] colors;
    private final float radius;

    private float value; // 0..1
    private Listener listener;

    /** @param colors the colours of the bar from its left end to its right end */
    ColorSlider(Context context, int[] colors) {
        super(context);
        this.colors = colors;
        setFocusable(true);
        setClickable(true);
        float density = getResources().getDisplayMetrics().density;
        radius = 8 * density;
        marker.setStyle(Paint.Style.STROKE);
        marker.setStrokeWidth(3 * density);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(3 * density);
        outline.setColor(context.getColor(R.color.text_primary));
        marker.setColor(0xFFFFFFFF);
    }

    void setColors(int[] newColors) {
        System.arraycopy(newColors, 0, colors, 0, Math.min(colors.length, newColors.length));
        track.setShader(null);
        invalidate();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    float value() {
        return value;
    }

    void setValue(float newValue) {
        value = Math.max(0f, Math.min(1f, newValue));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = outline.getStrokeWidth() * 1.5f;
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        if (track.getShader() == null) {
            track.setShader(new LinearGradient(bounds.left, 0, bounds.right, 0, colors, null, Shader.TileMode.CLAMP));
        }
        canvas.drawRoundRect(bounds, radius, radius, track);
        if (isFocused()) {
            RectF ring = new RectF(bounds);
            ring.inset(-inset / 1.5f, -inset / 1.5f);
            canvas.drawRoundRect(ring, radius, radius, outline);
        }
        float x = bounds.left + value * bounds.width();
        marker.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(x - 4 * marker.getStrokeWidth() / 3, bounds.top, x + 4 * marker.getStrokeWidth() / 3,
                bounds.bottom, radius / 2, radius / 2, marker);
        marker.setColor(0x99000000);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        track.setShader(null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        float step = 1f / STEPS * (1 + Math.min(event.getRepeatCount() / 4, 4));
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            setValue(value + (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -step : step));
            if (listener != null) {
                listener.onValueChanged();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
