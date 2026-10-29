package com.nison.freedeck.ui.loader;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.nison.freedeck.R;

import java.io.IOException;

/**
 * Full-screen loading animation. Loops until {@link #stop()} is called.
 * <p>
 * The animation is white on black. It is drawn in the theme's own colours (the background colour where it is black,
 * the text colour where it is white), and enlarged so the dot grid in its middle fills the whole 16:9 screen.
 */
public class LoaderView extends FrameLayout {

    /** The grid in the video is square and about 54% of the frame's width; this makes it reach both screen edges. */
    private static final float FILL_SCALE = 1.85f;

    private final TextureView video;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private boolean prepared;
    private boolean wantPlaying;
    private Runnable onFirstLoopCompleted;

    public LoaderView(Context context) {
        this(context, null);
    }

    public LoaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_loader, this, true);
        video = findViewById(R.id.loader_video);
        video.setOpaque(true);
        video.setLayerType(LAYER_TYPE_HARDWARE, themePaint(context));
        video.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            video.setPivotX((r - l) / 2f);
            video.setPivotY((b - t) / 2f);
        });
        video.setScaleX(FILL_SCALE);
        video.setScaleY(FILL_SCALE);
        video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                prepare(new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                release();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    /** Maps the video's black to the theme background and its white to the theme text colour. */
    private static Paint themePaint(Context context) {
        int dark = context.getColor(R.color.background);
        int light = context.getColor(R.color.text_primary);
        float[] matrix = new float[20];
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - 8 * channel;
            float low = ((dark >> shift) & 0xFF);
            float high = ((light >> shift) & 0xFF);
            // out = low + brightness * (high - low), where brightness is the video pixel's own (grey) value.
            matrix[channel * 5 + channel] = (high - low) / 255f;
            matrix[channel * 5 + 4] = low;
        }
        matrix[18] = 1f;
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(matrix)));
        return paint;
    }

    private void prepare(Surface surface) {
        release();
        MediaPlayer created = new MediaPlayer();
        try {
            android.content.res.AssetFileDescriptor file =
                    getResources().openRawResourceFd(R.raw.loader_animation);
            created.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            file.close();
            created.setSurface(surface);
            created.setLooping(true);
            created.setOnPreparedListener(mp -> {
                prepared = true;
                // The animation loops seamlessly, so there is no "finished" event; count one full pass by its length.
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(this::notifyFirstLoopCompleted, mp.getDuration());
                if (wantPlaying) {
                    mp.start();
                }
            });
            created.prepareAsync();
            player = created;
        } catch (IOException | RuntimeException e) {
            created.release();
        }
    }

    private void release() {
        prepared = false;
        if (player != null) {
            player.release();
            player = null;
        }
    }

    /** Called once, after the animation has played all the way through for the first time. */
    public void setOnFirstLoopCompleted(Runnable listener) {
        onFirstLoopCompleted = listener;
    }

    private void notifyFirstLoopCompleted() {
        Runnable listener = onFirstLoopCompleted;
        onFirstLoopCompleted = null;
        if (listener != null) {
            listener.run();
        }
    }

    /** Shows the loader and plays the animation from the start. */
    public void start() {
        setVisibility(VISIBLE);
        wantPlaying = true;
        if (prepared && player != null) {
            player.seekTo(0);
            player.start();
        }
    }

    /** Stops playback and hides the loader. */
    public void stop() {
        wantPlaying = false;
        handler.removeCallbacksAndMessages(null);
        if (prepared && player != null) {
            player.pause();
        }
        setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        release();
        super.onDetachedFromWindow();
    }
}
