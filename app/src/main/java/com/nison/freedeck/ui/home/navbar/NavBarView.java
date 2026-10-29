package com.nison.freedeck.ui.home.navbar;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.location.Place;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.ClockFormat;
import com.nison.freedeck.data.settings.NavButton;
import com.nison.freedeck.data.settings.NavbarItem;
import com.nison.freedeck.data.weather.Weather;
import com.nison.freedeck.startup.HomeData;
import com.nison.freedeck.system.network.NetworkState;
import com.nison.freedeck.ui.home.Page;

import java.text.DateFormat;
import java.time.LocalTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Top bar:
 * <pre>
 * Greeting | Time | Date           [Home] [Apps] [Settings] [Wi-Fi]
 * Location | Weather icon + temp
 * </pre>
 * Every button selects its page as soon as it takes focus or is clicked.
 * The Wi-Fi icon is also tinted to show the network status.
 */
public class NavBarView extends FrameLayout {

    /** Receives the page the user has moved to. */
    public interface OnPageSelectedListener {
        void onPageSelected(Page page);
    }

    /** The clock and greeting are re-read this often; the clock is checked often enough never to be a second late. */
    private static final long HIGHLIGHT_MS = 340L;
    private static final float HIGHLIGHT_IDLE_ALPHA = 0.2f;

    private static final long REFRESH_MS = 1_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshGreeting = new Runnable() {
        @Override
        public void run() {
            greeting.setText(Greeting.now());
            updateClock();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    private final Map<Page, NavIconButton> pageButtons = new EnumMap<>(Page.class);

    private final TextView greeting;
    private final TextView time;
    private final TextView timeSuffix;
    private final TextView date;
    private final View timeGroup;
    private final View dividerTime;
    private final View dividerDate;
    private final ViewGroup topRow;
    private final ViewGroup bottomRow;
    private final TextView location;
    private final View weatherDivider;
    private final View weatherGroup;
    private final ViewGroup buttonRow;
    private final ImageView weatherIcon;
    private final TextView temperature;
    private final WifiButton wifiButton;
    private final View highlight;
    private Page selectedPage = Page.DEFAULT;
    private boolean highlightPlaced;

    private OnPageSelectedListener listener;
    private AppSettings settings;
    private HomeData data;

    public NavBarView(Context context) {
        this(context, null);
    }

    public NavBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_navbar, this, true);
        greeting = findViewById(R.id.navbar_greeting);
        time = findViewById(R.id.navbar_time);
        timeSuffix = findViewById(R.id.navbar_time_suffix);
        date = findViewById(R.id.navbar_date);
        timeGroup = findViewById(R.id.navbar_time_group);
        dividerTime = findViewById(R.id.navbar_divider_time);
        dividerDate = findViewById(R.id.navbar_divider_date);
        topRow = findViewById(R.id.navbar_top_row);
        bottomRow = findViewById(R.id.navbar_bottom_row);
        location = findViewById(R.id.navbar_location);
        weatherDivider = findViewById(R.id.navbar_weather_divider);
        weatherGroup = findViewById(R.id.navbar_weather_group);
        buttonRow = findViewById(R.id.navbar_buttons);
        highlight = findViewById(R.id.nav_highlight);
        // Place (and keep placed) the highlight whenever the buttons are laid out, e.g. after being reordered.
        buttonRow.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> post(() -> moveHighlight(false)));
        weatherIcon = findViewById(R.id.navbar_weather_icon);
        temperature = findViewById(R.id.navbar_temperature);
        wifiButton = findViewById(R.id.nav_wifi);

        pageButtons.put(Page.HOME, findViewById(R.id.nav_home));
        pageButtons.put(Page.APPS, findViewById(R.id.nav_apps));
        pageButtons.put(Page.SETTINGS, findViewById(R.id.nav_settings));
        pageButtons.put(Page.WIFI, wifiButton);
        for (Map.Entry<Page, NavIconButton> entry : pageButtons.entrySet()) {
            bindPageButton(entry.getValue(), entry.getKey());
        }

        markSelected(Page.DEFAULT);
        bind(null);
    }

    public void setOnPageSelectedListener(OnPageSelectedListener listener) {
        this.listener = listener;
    }

    /** Puts focus on Home and selects it. */
    public void focusHome() {
        pageButtons.get(Page.HOME).requestFocus();
        select(Page.HOME);
    }

    /** Puts focus on Settings and selects it. */
    public void focusSettings() {
        pageButtons.get(Page.SETTINGS).requestFocus();
        select(Page.SETTINGS);
    }

    /** Follows the clock format and the show / hide choices from now on, and applies them straight away. */
    public void bindSettings(AppSettings settings) {
        this.settings = settings;
        settings.addNavbarListener(this::applySettings);
        applySettings();
    }

    public void bind(HomeData data) {
        this.data = data;
        bindLocation(data != null ? data.place : null);
        bindWeather(data != null ? data.weather : null);
        applySettings();
    }

    private void updateClock() {
        ClockFormat format = settings != null ? settings.clockFormat() : ClockFormat.DEFAULT;
        ClockFormatter.Parts parts = ClockFormatter.parts(LocalTime.now(), format, Locale.ENGLISH);
        time.setText(parts.time);
        timeSuffix.setText(parts.suffix);
        timeSuffix.setVisibility(parts.suffix.isEmpty() ? GONE : VISIBLE);
        date.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH).format(new Date()));
    }

    /**
     * Puts each piece of text where the user wants it (the first three on the top row, the rest on the bottom
     * row), shows or hides it, and keeps dividers only between pieces that are both on screen. The icon buttons
     * are put in the chosen order too.
     */
    private void applySettings() {
        if (settings == null) {
            return;
        }
        List<NavbarItem> order = settings.navbarTextOrder();
        View[] topDividers = {dividerTime};
        View[] bottomDividers = {dividerDate, weatherDivider};
        topRow.removeAllViews();
        bottomRow.removeAllViews();
        fillRow(topRow, order.subList(0, NavbarItem.FIRST_LINE_COUNT), topDividers);
        fillRow(bottomRow, order.subList(NavbarItem.FIRST_LINE_COUNT, order.size()), bottomDividers);
        applyButtonOrder();
        updateClock();
    }

    private void fillRow(ViewGroup row, List<NavbarItem> items, View[] dividers) {
        boolean[] shown = new boolean[items.size()];
        for (int i = 0; i < shown.length; i++) {
            shown[i] = isShown(items.get(i));
        }
        boolean[] drawn = NavbarLayout.dividersBefore(shown);
        for (int i = 0; i < shown.length; i++) {
            if (i > 0) {
                dividers[i - 1].setVisibility(drawn[i - 1] ? VISIBLE : GONE);
                row.addView(dividers[i - 1]);
            }
            View piece = viewOf(items.get(i));
            piece.setVisibility(shown[i] ? VISIBLE : GONE);
            row.addView(piece);
        }
        row.setVisibility(NavbarLayout.anyShown(shown) ? VISIBLE : GONE);
    }

    private boolean isShown(NavbarItem item) {
        if (!settings.isNavbarItemShown(item)) {
            return false;
        }
        return item != NavbarItem.WEATHER || (data != null && data.weather != null);
    }

    private View viewOf(NavbarItem item) {
        switch (item) {
            case GREETING:
                return greeting;
            case TIME:
                return timeGroup;
            case DATE:
                return date;
            case LOCATION:
                return location;
            default:
                return weatherGroup;
        }
    }

    private void applyButtonOrder() {
        List<NavButton> order = settings.navButtonOrder();
        buttonRow.removeAllViews();
        for (NavButton button : order) {
            buttonRow.addView(pageButtons.get(pageOf(button)));
        }
        // Left and right go round the buttons in the order they are in now.
        for (int i = 0; i < order.size(); i++) {
            View button = pageButtons.get(pageOf(order.get(i)));
            button.setNextFocusLeftId(pageButtons.get(pageOf(order.get((i + order.size() - 1) % order.size()))).getId());
            button.setNextFocusRightId(pageButtons.get(pageOf(order.get((i + 1) % order.size()))).getId());
        }
    }

    private static Page pageOf(NavButton button) {
        switch (button) {
            case HOME:
                return Page.HOME;
            case APPS:
                return Page.APPS;
            case SETTINGS:
                return Page.SETTINGS;
            default:
                return Page.WIFI;
        }
    }

    public void bindNetwork(NetworkState state) {
        wifiButton.setState(state);
    }

    private void bindPageButton(NavIconButton button, Page page) {
        button.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                select(page);
                moveHighlight(true);
            } else {
                // Focus may be moving to another button (the highlight follows) or off the bar (it fades back).
                post(() -> moveHighlight(true));
            }
        });
        button.setOnClickListener(v -> select(page));
    }

    private void select(Page page) {
        selectedPage = page;
        markSelected(page);
        if (listener != null) {
            listener.onPageSelected(page);
        }
    }

    /** The button that has focus, or null when focus is somewhere else. */
    private NavIconButton focusedButton() {
        for (NavIconButton button : pageButtons.values()) {
            if (button.hasFocus()) {
                return button;
            }
        }
        return null;
    }

    /**
     * Slides the highlight to the focused button (solid), or, when focus is off the bar, to the open page's button
     * (faint). It stretches a little while it travels, so it reads as one shape moving rather than hopping.
     */
    private void moveHighlight(boolean animate) {
        NavIconButton focused = focusedButton();
        NavIconButton target = focused != null ? focused : pageButtons.get(selectedPage);
        if (target == null || target.getWidth() == 0) {
            return;
        }
        float x = target.getLeft();
        float alpha = focused != null ? 1f : HIGHLIGHT_IDLE_ALPHA;
        highlight.setVisibility(VISIBLE);
        highlight.animate().cancel();
        if (!animate || !highlightPlaced) {
            highlight.setTranslationX(x);
            highlight.setAlpha(alpha);
            highlight.setScaleX(1f);
            highlightPlaced = true;
            return;
        }
        float distance = Math.abs(x - highlight.getTranslationX());
        if (distance > 0.5f) {
            float stretch = 1f + Math.min(0.28f, distance / (highlight.getWidth() * 6f));
            android.animation.ObjectAnimator squash = android.animation.ObjectAnimator.ofFloat(
                    highlight, View.SCALE_X, 1f, stretch, 1f);
            squash.setDuration(HIGHLIGHT_MS);
            squash.start();
        }
        highlight.animate()
                .translationX(x)
                .alpha(alpha)
                .setDuration(HIGHLIGHT_MS)
                .setInterpolator(new android.view.animation.PathInterpolator(0.2f, 0.9f, 0.25f, 1f))
                .start();
    }

    private void markSelected(Page selected) {
        for (Map.Entry<Page, NavIconButton> entry : pageButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selected);
        }
    }

    private void bindLocation(Place place) {
        if (place == null || place.city.isEmpty()) {
            location.setText(R.string.location_unavailable);
        } else {
            location.setText(getContext().getString(R.string.location_format, place.city, place.region));
        }
    }

    private void bindWeather(Weather weather) {
        if (weather == null) {
            return;
        }
        weatherIcon.setImageResource(weather.condition.icon);
        weatherIcon.setContentDescription(getContext().getString(weather.condition.description));
        temperature.setText(String.format(Locale.ENGLISH, "%d°C", Math.round(weather.temperatureCelsius)));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshGreeting.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(refreshGreeting);
        super.onDetachedFromWindow();
    }
}
