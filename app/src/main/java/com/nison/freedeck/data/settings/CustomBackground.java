package com.nison.freedeck.data.settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The pictures the user uploaded as backgrounds: up to {@link #SLOTS} of them, each one file in the app's own
 * storage, shrunk to the screen size. They are called Custom 1 to Custom 5.
 */
public final class CustomBackground {

    public static final int SLOTS = 5;

    /** The most an upload may weigh. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final String LEGACY_FILE_NAME = "custom_background.jpg";
    private static final int SCREEN_WIDTH = 1920;
    private static final int SCREEN_HEIGHT = 1080;
    private static final int THUMBNAIL_SAMPLE = 8;
    private static final int JPEG_QUALITY = 90;

    private CustomBackground() {
    }

    private static File file(Context context, int slot) {
        return new File(context.getFilesDir(), "custom_background_" + slot + ".jpg");
    }

    /** The single picture kept before there were five becomes Custom 1. */
    private static void adoptLegacyFile(Context context) {
        File legacy = new File(context.getFilesDir(), LEGACY_FILE_NAME);
        if (legacy.isFile()) {
            File first = file(context, 1);
            if (first.isFile() || !legacy.renameTo(first)) {
                legacy.delete();
            }
        }
    }

    public static boolean exists(Context context, int slot) {
        adoptLegacyFile(context);
        return file(context, slot).isFile();
    }

    /** How many pictures are uploaded now. */
    public static int count(Context context) {
        int count = 0;
        for (int slot = 1; slot <= SLOTS; slot++) {
            if (exists(context, slot)) {
                count++;
            }
        }
        return count;
    }

    /** Where the next upload goes: the first empty slot, or, with all five used, the one uploaded longest ago. */
    public static int slotForUpload(Context context) {
        int oldest = 1;
        long oldestTime = Long.MAX_VALUE;
        for (int slot = 1; slot <= SLOTS; slot++) {
            if (!exists(context, slot)) {
                return slot;
            }
            long time = file(context, slot).lastModified();
            if (time < oldestTime) {
                oldestTime = time;
                oldest = slot;
            }
        }
        return oldest;
    }

    /**
     * Copies the picture at {@code source} into the app as Custom {@code slot}, shrunk to fit the screen. Blocking:
     * call it off the main thread. @return false if it is too big or could not be read as a picture
     */
    public static boolean save(Context context, int slot, Uri source) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                if (in == null) {
                    return false;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return false;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = ImageScale.sampleSize(bounds.outWidth, bounds.outHeight, SCREEN_WIDTH, SCREEN_HEIGHT);
            Bitmap decoded;
            try (InputStream in = context.getContentResolver().openInputStream(source)) {
                decoded = in == null ? null : BitmapFactory.decodeStream(in, null, options);
            }
            if (decoded == null) {
                return false;
            }
            int[] size = ImageScale.fit(decoded.getWidth(), decoded.getHeight(), SCREEN_WIDTH, SCREEN_HEIGHT);
            Bitmap scaled = size[0] == decoded.getWidth() ? decoded
                    : Bitmap.createScaledBitmap(decoded, size[0], size[1], true);
            File target = file(context, slot);
            File temp = new File(target.getParentFile(), target.getName() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
            }
            if (scaled != decoded) {
                scaled.recycle();
            }
            decoded.recycle();
            return temp.renameTo(target) || (target.delete() && temp.renameTo(target));
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            return false;
        }
    }

    /** Removes Custom {@code slot}. @return true if it is gone (also when there was nothing there) */
    public static boolean delete(Context context, int slot) {
        File file = file(context, slot);
        return !file.exists() || file.delete();
    }

    /** Custom {@code slot}, full size for the screen, or null if there is none. */
    @Nullable
    public static Drawable drawable(Context context, int slot) {
        return decode(context, slot, 1);
    }

    /** A small version for the picker, or null if there is none. */
    @Nullable
    public static Drawable thumbnail(Context context, int slot) {
        return decode(context, slot, THUMBNAIL_SAMPLE);
    }

    @Nullable
    private static Drawable decode(Context context, int slot, int sample) {
        if (!exists(context, slot)) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(file(context, slot).getAbsolutePath(), options);
        return bitmap == null ? null : new BitmapDrawable(context.getResources(), bitmap);
    }
}
