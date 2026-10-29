package com.nison.freedeck.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;

import com.nison.freedeck.R;

/** A "label ........ value" line. Set the label with {@code app:rowLabel}, the value from code. */
public class DetailRow extends LinearLayout {

    private final TextView value;

    public DetailRow(Context context) {
        this(context, null);
    }

    public DetailRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.view_detail_row, this, true);
        value = findViewById(R.id.detail_value);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.DetailRow);
        ((TextView) findViewById(R.id.detail_label)).setText(attributes.getText(R.styleable.DetailRow_rowLabel));
        attributes.recycle();
    }

    public void setValue(CharSequence text) {
        value.setText(text);
    }

    public void setValueColor(@ColorInt int color) {
        value.setTextColor(color);
    }
}
