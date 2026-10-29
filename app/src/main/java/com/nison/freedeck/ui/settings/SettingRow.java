package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** A focusable settings row: icon, title, an optional current value on the right, and a chevron. */
public class SettingRow extends LinearLayout {

    private final TextView title;
    private final TextView value;
    private final ImageView icon;
    private final ImageView chevron;
    private final ImageView toggle;

    public SettingRow(Context context) {
        this(context, null);
    }

    public SettingRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setClickable(true);
        setBackgroundResource(R.drawable.setting_row_background);
        int padding = getResources().getDimensionPixelSize(R.dimen.settings_row_padding);
        setPadding(padding, 0, padding, 0);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.settings_row_height));
        LayoutInflater.from(context).inflate(R.layout.view_setting_row, this, true);
        title = findViewById(R.id.setting_title);
        value = findViewById(R.id.setting_value);
        icon = findViewById(R.id.setting_icon);
        chevron = findViewById(R.id.setting_chevron);
        toggle = findViewById(R.id.setting_switch);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingRow);
        title.setText(attributes.getText(R.styleable.SettingRow_rowTitle));
        icon.setImageDrawable(attributes.getDrawable(R.styleable.SettingRow_rowIcon));
        attributes.recycle();
    }

    public void setTitle(@StringRes int text) {
        title.setText(text);
    }

    public void setIcon(@DrawableRes int drawable) {
        icon.setImageResource(drawable);
    }

    public void setValue(CharSequence text) {
        value.setText(text);
    }

    /** The arrow on the right; hidden for rows that change a value in place instead of opening something. */
    public void setChevronVisible(boolean visible) {
        chevron.setVisibility(visible ? VISIBLE : GONE);
    }

    /** Turns the row into an on / off switch: the switch replaces the value text and the arrow. */
    public void setSwitch(boolean on) {
        value.setVisibility(GONE);
        chevron.setVisibility(GONE);
        toggle.setVisibility(VISIBLE);
        toggle.setImageResource(on ? R.drawable.toggle_on : R.drawable.toggle_off);
        setContentDescription(title.getText() + ", " + getContext().getString(on ? R.string.switch_on : R.string.switch_off));
    }
}
