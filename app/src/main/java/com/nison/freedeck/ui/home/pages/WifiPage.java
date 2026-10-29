package com.nison.freedeck.ui.home.pages;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.location.Place;
import com.nison.freedeck.system.SystemSettings;
import com.nison.freedeck.system.network.NetworkState;
import com.nison.freedeck.system.network.Severity;
import com.nison.freedeck.system.network.SignalQuality;
import com.nison.freedeck.system.network.WifiBand;
import com.nison.freedeck.ui.common.DetailRow;
import com.nison.freedeck.ui.map.WorldMapView;

/**
 * A world map with a pin at the TV's location, joined by a line to a card with the Wi-Fi details
 * and a button into the system Network settings.
 */
public class WifiPage extends FrameLayout {

    private final WorldMapView map;
    private final View card;
    private final ImageView cardIcon;
    private final TextView name;
    private final TextView location;
    private final DetailRow status;
    private final DetailRow signal;
    private final DetailRow band;
    private final DetailRow ipAddress;
    private final float cardGap;
    private final float edgeMargin;
    private final float anchorInset;
    private final float topInset;
    private final float bottomInset;

    public WifiPage(Context context) {
        this(context, null);
    }

    public WifiPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.page_wifi, this, true);
        map = findViewById(R.id.wifi_map);
        card = findViewById(R.id.wifi_card);
        cardIcon = findViewById(R.id.wifi_card_icon);
        name = findViewById(R.id.wifi_name);
        location = findViewById(R.id.wifi_location);
        status = findViewById(R.id.wifi_status);
        signal = findViewById(R.id.wifi_signal);
        band = findViewById(R.id.wifi_band);
        ipAddress = findViewById(R.id.wifi_ip);
        findViewById(R.id.wifi_more_settings).setOnClickListener(v -> SystemSettings.openWifi(context));

        cardGap = getResources().getDimension(R.dimen.wifi_card_gap);
        edgeMargin = getResources().getDimension(R.dimen.screen_margin_horizontal);
        anchorInset = getResources().getDimension(R.dimen.wifi_card_anchor_inset);
        topInset = getResources().getDimension(R.dimen.navbar_height);
        bottomInset = getResources().getDimension(R.dimen.screen_margin_vertical);

        bind(NetworkState.disconnected());
        bindPlace(null);
    }

    public void bindPlace(Place place) {
        if (place == null) {
            map.clearLocation();
            location.setText(R.string.location_unavailable);
        } else {
            map.setLocation(place.latitude, place.longitude);
            location.setText(place.city.isEmpty()
                    ? getContext().getString(R.string.location_unavailable)
                    : getContext().getString(R.string.location_format, place.city, place.region));
        }
        requestLayout();
    }

    public void bind(NetworkState state) {
        name.setText(titleFor(state));
        cardIcon.setImageTintList(getContext().getColorStateList(state.status.severity.tint));
        cardIcon.setContentDescription(getContext().getString(state.status.description));

        status.setValue(getContext().getString(state.status.description));
        status.setValueColor(colorOf(state.status.severity));

        boolean hasSignal = state.signalDbm != NetworkState.NO_VALUE;
        if (hasSignal) {
            SignalQuality quality = SignalQuality.fromDbm(state.signalDbm);
            signal.setValue(getContext().getString(R.string.wifi_signal_format,
                    getContext().getString(quality.label), state.signalDbm));
            signal.setValueColor(colorOf(quality.severity));
        }
        signal.setVisibility(hasSignal ? View.VISIBLE : View.GONE);

        WifiBand wifiBand = WifiBand.fromMhz(state.frequencyMhz == NetworkState.NO_VALUE ? 0 : state.frequencyMhz);
        bindRow(band, wifiBand != WifiBand.UNKNOWN, wifiBand.label);

        bindRow(ipAddress, state.ipAddress != null, state.ipAddress);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        positionCard();
    }

    /** Puts the card beside the pin, on whichever side has more room, and joins them with a line. */
    private void positionCard() {
        float width = getWidth();
        float height = getHeight();
        float cardWidth = card.getMeasuredWidth();
        float cardHeight = card.getMeasuredHeight();
        PointF pin = map.getPinPosition();

        float x;
        float y;
        if (pin == null) {
            x = (width - cardWidth) / 2f;
            y = (height - cardHeight) / 2f;
            map.clearConnectorEnd();
        } else {
            boolean cardOnLeft = pin.x > width / 2f;
            x = cardOnLeft ? pin.x - cardGap - cardWidth : pin.x + cardGap;
            y = pin.y - cardHeight / 2f;
        }
        x = clamp(x, edgeMargin, width - edgeMargin - cardWidth);
        y = clamp(y, topInset, height - bottomInset - cardHeight);
        card.setTranslationX(x - card.getLeft());
        card.setTranslationY(y - card.getTop());

        if (pin != null) {
            float anchorX = pin.x > x + cardWidth / 2f ? x + cardWidth : x;
            float anchorY = clamp(pin.y, y + anchorInset, y + cardHeight - anchorInset);
            map.setConnectorEnd(anchorX, anchorY);
        }
    }

    private String titleFor(NetworkState state) {
        if (!state.isConnected()) {
            return getContext().getString(R.string.network_none);
        }
        if (!state.onWifi) {
            return getContext().getString(R.string.network_wired);
        }
        return state.ssid != null ? state.ssid : getContext().getString(R.string.wifi_name_hidden);
    }

    private int colorOf(Severity severity) {
        return getContext().getColorStateList(severity.tint).getDefaultColor();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void bindRow(DetailRow row, boolean visible, CharSequence value) {
        row.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            row.setValue(value);
        }
    }
}
