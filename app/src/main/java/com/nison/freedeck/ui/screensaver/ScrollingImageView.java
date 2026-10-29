package com.nison.freedeck.ui.screensaver;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/**
 * Shows a picture as if wrapped round a cylinder that turns: it scrolls sideways at a steady speed, forever, in one
 * direction. The picture is laid out next to its own mirror image, so the left and right edges always meet
 * (a mirrored strip repeats every two widths), whatever the picture is.
 */
class ScrollingImageView extends ImageView {

    /** How far it moves per second, as a share of the screen width. */
    private static final float SPEED = 0.025f;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Matrix matrix = new Matrix();
    private ValueAnimator animator;
    private BitmapShader shader;
    private Bitmap shaderBitmap;
    private float offset;
    private boolean scrolling;

    ScrollingImageView(Context context) {
        super(context);
    }

    /** Turns the scrolling on or off; it only applies to a picture that is a bitmap. */
    void setScrolling(boolean on) {
        scrolling = on;
        stopAnimator();
        offset = 0f;
        invalidate();
    }

    /** Starts (or restarts) the endless turn, once the view has a size. */
    void startScrolling() {
        stopAnimator();
        Bitmap bitmap = bitmap();
        if (!scrolling || bitmap == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float scale = (float) getHeight() / bitmap.getHeight();
        float period = 2f * bitmap.getWidth() * scale; // the picture and its mirror
        long duration = (long) (period / (getWidth() * SPEED) * 1000f);
        animator = ValueAnimator.ofFloat(0f, period);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(a -> {
            offset = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    void stopAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private Bitmap bitmap() {
        return getDrawable() instanceof BitmapDrawable ? ((BitmapDrawable) getDrawable()).getBitmap() : null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = bitmap();
        if (!scrolling || bitmap == null || animator == null) {
            super.onDraw(canvas);
            return;
        }
        if (shaderBitmap != bitmap) {
            shader = new BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.CLAMP);
            shaderBitmap = bitmap;
            paint.setShader(shader);
        }
        float scale = (float) getHeight() / bitmap.getHeight();
        matrix.setScale(scale, scale);
        matrix.postTranslate(-offset, 0f);
        shader.setLocalMatrix(matrix);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (scrolling && animator != null) {
            startScrolling();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimator();
        super.onDetachedFromWindow();
    }
}
