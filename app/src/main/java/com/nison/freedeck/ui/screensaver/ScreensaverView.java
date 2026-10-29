package com.nison.freedeck.ui.screensaver;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppBackground;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.ClockFormat;
import com.nison.freedeck.data.settings.CustomBackground;
import com.nison.freedeck.data.settings.ScreensaverWidget;
import com.nison.freedeck.startup.HomeData;
import com.nison.freedeck.ui.common.BackgroundDrawables;
import com.nison.freedeck.ui.home.navbar.Greeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The launcher's screensaver: a black layer with the chosen background (one picture, or all of them in turn when
 * random is on) and the chosen widgets, pinned to the bottom-left corner.
 */
public class ScreensaverView extends FrameLayout {

    private static final long SLIDE_MS = 20_000L;
    private static final long FADE_MS = 1_500L;
    private static final float PICTURE_DIM = 0.6f;

    /** Text heights as a share of the screen height, and the most of the screen width the widgets may take. */
    private static final float BIG_TEXT = 0.05f;
    private static final float SMALL_TEXT = 0.024f;
    private static final float MAX_SHARE = 0.5f;

    private static final String DIVIDER = "divider";

    private final LinearLayout block;
    private final List<TextView> bigTexts = new ArrayList<>();
    private final List<TextView> smallTexts = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final ScrollingImageView picture;
    private final TextClock clock;
    private final TextClock date;
    private final TextView greeting;
    private final TextView weather;
    private final ImageView weatherIcon;
    private final LinearLayout weatherGroup;
    private final TextView location;
    private final LinearLayout topRow;
    private final LinearLayout bottomRow;
    private boolean motion;
    /** Whether the picture on screen may move: only the built-in scenes, never an uploaded picture. */
    private boolean pictureMoves;
    private final List<AppBackground> slides = new ArrayList<>();
    private int slideIndex;
    private boolean running;

    private final Runnable nextSlide = new Runnable() {
        @Override
        public void run() {
            if (!running || slides.size() < 2) {
                return;
            }
            slideIndex = (slideIndex + 1) % slides.size();
            showPicture(BackgroundDrawables.create(getContext(), slides.get(slideIndex)), slides.get(slideIndex).isScene());
            handler.postDelayed(this, SLIDE_MS);
        }
    };

    public ScreensaverView(Context context) {
        this(context, null);
    }

    public ScreensaverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.BLACK);
        setClickable(true);
        picture = new ScrollingImageView(context);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setAlpha(0f);
        addView(picture, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        clock = new TextClock(context);
        style(clock, 1);
        date = new TextClock(context);
        style(date, 1);
        date.setFormat12Hour("EEE, d MMM");
        date.setFormat24Hour("EEE, d MMM");
        greeting = text(context, 1);
        location = text(context, 1);
        weather = text(context, 1);
        bigTexts.add(clock);
        bigTexts.add(greeting);
        smallTexts.add(date);
        smallTexts.add(location);
        smallTexts.add(weather);

        // The weather is the same icon as the navbar's, then the temperature.
        weatherIcon = new ImageView(context);
        weatherIcon.setColorFilter(Color.WHITE);
        weatherGroup = new LinearLayout(context);
        weatherGroup.setGravity(Gravity.CENTER_VERTICAL);
        weatherGroup.addView(weatherIcon);
        weatherGroup.addView(weather);

        // Pinned to the bottom-left corner:  Greeting | Time  over  Date | Location | Weather.
        topRow = row(context);
        bottomRow = row(context);
        block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.addView(topRow);
        block.addView(bottomRow);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        params.setMargins(margin, margin, margin, margin);
        addView(block, params);
        setVisibility(GONE);
    }

    private static void style(TextView view, float unused) {
        view.setTextColor(Color.WHITE);
        view.setShadowLayer(12f, 0f, 0f, Color.BLACK);
        view.setGravity(Gravity.CENTER);
    }

    private LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    /** Puts the visible items in a row with a "|" between each pair; hidden items take no room. */
    private void fill(LinearLayout row, View... items) {
        row.removeAllViews();
        boolean first = true;
        for (View item : items) {
            if (item.getVisibility() != VISIBLE) {
                continue;
            }
            if (!first) {
                TextView divider = text(getContext(), 1);
                divider.setTag(DIVIDER);
                divider.setText("|");
                divider.setAlpha(0.6f);
                row.addView(divider);
            }
            if (item.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) item.getParent()).removeView(item);
            }
            row.addView(item);
            first = false;
        }
    }

    private static TextView text(Context context, float sp) {
        TextView view = new TextView(context);
        style(view, sp);
        return view;
    }

    /** Sizes the text from the screen's height, then shrinks it until the widgets take no more than half the width. */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);
        if (width > 0 && height > 0 && getVisibility() == VISIBLE) {
            float factor = 1f;
            for (int i = 0; i < 6; i++) {
                applySizes(factor);
                block.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
                float limit = width * MAX_SHARE;
                if (block.getMeasuredWidth() <= limit) {
                    break;
                }
                factor *= limit / block.getMeasuredWidth() * 0.98f;
            }
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    private void applySizes(float factor) {
        int height = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
        for (TextView view : bigTexts) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * BIG_TEXT * factor);
        }
        for (TextView view : smallTexts) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * SMALL_TEXT * factor);
        }
        int icon = (int) (height * SMALL_TEXT * factor * 1.5f);
        weatherIcon.setLayoutParams(new LinearLayout.LayoutParams(icon, icon));
        weather.setPadding((int) (icon * 0.2f), 0, 0, 0);
        sizeDividers(topRow, height * BIG_TEXT * factor);
        sizeDividers(bottomRow, height * SMALL_TEXT * factor);
    }

    private static void sizeDividers(LinearLayout row, float textPx) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (DIVIDER.equals(child.getTag())) {
                ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
                int pad = (int) (textPx * 0.5f);
                child.setPadding(pad, 0, pad, 0);
            }
        }
    }

    public boolean isShowing() {
        return running;
    }

    /** @param data what the home screen loaded (place and weather); either part may be missing */
    public void start(AppSettings settings, HomeData data) {
        stop();
        running = true;

        boolean h12 = settings.clockFormat() == ClockFormat.HOURS_12;
        clock.setFormat12Hour(h12 ? "h:mm a" : null);
        clock.setFormat24Hour(h12 ? null : "HH:mm");
        clock.setVisibility(visible(settings.isScreensaverWidgetShown(ScreensaverWidget.CLOCK)));
        date.setVisibility(visible(settings.isScreensaverWidgetShown(ScreensaverWidget.DATE)));
        greeting.setText(Greeting.now());
        greeting.setVisibility(visible(settings.isScreensaverWidgetShown(ScreensaverWidget.GREETING)));
        boolean hasWeather = data != null && data.weather != null;
        weatherGroup.setVisibility(visible(hasWeather && settings.isScreensaverWidgetShown(ScreensaverWidget.WEATHER)));
        if (hasWeather) {
            weatherIcon.setImageResource(data.weather.condition.icon);
            weatherIcon.setContentDescription(getContext().getString(data.weather.condition.description));
            weather.setText(String.format(Locale.ENGLISH, "%d°C", Math.round(data.weather.temperatureCelsius)));
        }
        boolean hasPlace = data != null && data.place != null && !data.place.city.isEmpty();
        location.setVisibility(visible(hasPlace && settings.isScreensaverWidgetShown(ScreensaverWidget.LOCATION)));
        if (hasPlace) {
            location.setText(getContext().getString(R.string.location_format, data.place.city, data.place.region));
        }

        fill(topRow, greeting, clock);
        fill(bottomRow, date, location, weatherGroup);
        applySizes(1f);

        motion = settings.isScreensaverMotion();
        slides.clear();
        slideIndex = 0;
        picture.animate().cancel();
        resetMotion();
        picture.setImageDrawable(null);
        picture.setAlpha(0f);
        if (settings.isScreensaverRandom()) {
            slides.addAll(BackgroundDrawables.pool(getContext(), settings.screensaverRandom()));
            java.util.Collections.shuffle(slides, random);
        } else if (!settings.screensaverBackground().isNone()) {
            slides.add(settings.screensaverBackground());
        }

        // The first picture is in place before the screensaver appears, so it and the widgets show together.
        if (!slides.isEmpty()) {
            Drawable first = BackgroundDrawables.create(getContext(), slides.get(0));
            pictureMoves = slides.get(0).isScene();
            picture.setImageDrawable(first);
            picture.setAlpha(first != null ? PICTURE_DIM : 0f);
            if (slides.size() > 1) {
                handler.postDelayed(nextSlide, SLIDE_MS);
            }
        }
        setVisibility(VISIBLE);
        bringToFront();
        requestLayout();
        startMotion();
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        picture.animate().cancel();
        resetMotion();
        slides.clear();
        picture.setImageDrawable(null);
        setVisibility(GONE);
    }

    private static int visible(boolean shown) {
        return shown ? VISIBLE : GONE;
    }

    private void resetMotion() {
        picture.setScrolling(false);
    }

    /**
     * Turns the picture like a cylinder, steadily and in one direction, for as long as the screensaver runs. Only for
     * the built-in scenes, and only with Live Motion on.
     */
    private void startMotion() {
        if (!motion || !pictureMoves || !running || picture.getDrawable() == null) {
            return;
        }
        picture.setScrolling(true);
        if (getWidth() == 0 || getHeight() == 0) {
            // Not laid out yet (the screensaver was hidden until now): the speed depends on the size.
            post(this::startMotion);
            return;
        }
        picture.startScrolling();
    }

    private void showPicture(Drawable drawable, boolean moves) {
        if (drawable == null) {
            return;
        }
        picture.animate().alpha(0f).setDuration(FADE_MS / 2).withEndAction(() -> {
            picture.setImageDrawable(drawable);
            pictureMoves = moves;
            resetMotion();
            picture.animate().alpha(PICTURE_DIM).setDuration(FADE_MS).start();
            startMotion();
        }).start();
    }
}
