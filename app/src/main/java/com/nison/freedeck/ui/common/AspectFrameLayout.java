package com.nison.freedeck.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.nison.freedeck.R;

/**
 * A FrameLayout whose height follows its width ({@code app:aspectRatio}, width / height) and that can
 * round its contents' corners ({@code app:cornerRadius}).
 * <p>
 * The corners are cut by clipping the canvas rather than by an outline, so they stay round when the
 * view is scaled, for example by a focus animation.
 */
public class AspectFrameLayout extends FrameLayout {

    private final float aspectRatio;
    private final float cornerRadius;
    private final Path clip = new Path();

    public AspectFrameLayout(Context context) {
        this(context, null);
    }

    public AspectFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.AspectFrameLayout);
        aspectRatio = attributes.getFloat(R.styleable.AspectFrameLayout_aspectRatio, 1f);
        cornerRadius = attributes.getDimension(R.styleable.AspectFrameLayout_cornerRadius, 0f);
        attributes.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width / aspectRatio);
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        clip.reset();
        clip.addRoundRect(new RectF(0, 0, width, height), cornerRadius, cornerRadius, Path.Direction.CW);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (cornerRadius <= 0f) {
            super.dispatchDraw(canvas);
            return;
        }
        int saved = canvas.save();
        canvas.clipPath(clip);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(saved);
    }
}
