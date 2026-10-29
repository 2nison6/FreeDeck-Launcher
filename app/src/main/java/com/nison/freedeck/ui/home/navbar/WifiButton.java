package com.nison.freedeck.ui.home.navbar;

import android.content.Context;
import android.util.AttributeSet;

import com.nison.freedeck.R;
import com.nison.freedeck.system.network.NetworkState;

/** Nav button whose icon is tinted green, orange or red by whether the device is online. */
public class WifiButton extends NavIconButton {

    public WifiButton(Context context) {
        this(context, null);
    }

    public WifiButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setState(NetworkState state) {
        setImageTintList(getContext().getColorStateList(state.status.severity.tint));
        setContentDescription(getContext().getString(R.string.nav_wifi_status, getContext().getString(state.status.description)));
    }
}
