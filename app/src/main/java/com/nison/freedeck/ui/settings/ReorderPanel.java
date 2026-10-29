package com.nison.freedeck.ui.settings;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.settings.AppSettings;
import com.nison.freedeck.data.settings.NavButton;
import com.nison.freedeck.data.settings.NavbarItem;

import java.util.List;

/**
 * A small live copy of the navbar's layout above two lists to rearrange: the text (the first three go on the top
 * row, the rest underneath, exactly as in the grid) and the icon buttons. The real navbar follows each move too.
 */
class ReorderPanel extends LinearLayout implements DropdownContent {

    private static final int TEXT_COLUMNS = 3;

    private final AppSettings settings;
    private final LinearLayout preview;
    private final OrderGrid<NavbarItem> textGrid;
    private final OrderGrid<NavButton> buttonGrid;
    private View upExit;
    private View downExit;

    ReorderPanel(Context context, AppSettings settings, Runnable onChanged) {
        super(context);
        this.settings = settings;
        setOrientation(VERTICAL);

        preview = new LinearLayout(context);
        preview.setOrientation(VERTICAL);
        preview.setBackgroundResource(R.drawable.setting_chip_background);
        int pad = getResources().getDimensionPixelSize(R.dimen.settings_row_padding);
        preview.setPadding(pad, pad / 2, pad, pad / 2);

        textGrid = new OrderGrid<>(context, settings.navbarTextOrder(), TEXT_COLUMNS,
                item -> context.getString(item.label), settings::isNavbarItemShown, order -> {
                    settings.setNavbarTextOrder(order);
                    refreshPreview();
                    onChanged.run();
                });
        buttonGrid = new OrderGrid<>(context, settings.navButtonOrder(), NavButton.values().length,
                button -> context.getString(button.label), button -> true, order -> {
                    settings.setNavButtonOrder(order);
                    refreshPreview();
                    onChanged.run();
                });

        TextView hint = new TextView(context);
        hint.setText(R.string.reorder_hint);
        hint.setTextColor(context.getColor(R.color.text_secondary));
        hint.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text));
        hint.setPadding(0, getResources().getDimensionPixelSize(R.dimen.chip_gap), 0,
                getResources().getDimensionPixelSize(R.dimen.chip_gap));

        addView(preview);
        addView(hint);
        addView(heading(context, R.string.reorder_heading_text));
        addView(textGrid);
        addView(heading(context, R.string.reorder_heading_buttons));
        addView(buttonGrid);
        refreshPreview();
        wireExits();
    }

    private TextView heading(Context context, int text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(context.getColor(R.color.text_secondary));
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text));
        view.setPadding(0, getResources().getDimensionPixelSize(R.dimen.chip_gap), 0,
                getResources().getDimensionPixelSize(R.dimen.chip_gap));
        return view;
    }

    /** Redraws the mini navbar: the text rows on the left, the buttons on the right. */
    private void refreshPreview() {
        Context context = getContext();
        preview.removeAllViews();
        LinearLayout line = new LinearLayout(context);
        line.setOrientation(HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        List<NavbarItem> order = settings.navbarTextOrder();
        texts.addView(previewText(context, order.subList(0, NavbarItem.FIRST_LINE_COUNT)));
        texts.addView(previewText(context, order.subList(NavbarItem.FIRST_LINE_COUNT, order.size())));
        line.addView(texts, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        int icon = getResources().getDimensionPixelSize(R.dimen.chip_text) * 3 / 2;
        for (NavButton button : settings.navButtonOrder()) {
            ImageView view = new ImageView(context);
            view.setImageResource(iconOf(button));
            LayoutParams params = new LayoutParams(icon, icon);
            params.setMarginStart(icon / 2);
            line.addView(view, params);
        }
        preview.addView(line);
    }

    private TextView previewText(Context context, List<NavbarItem> items) {
        StringBuilder text = new StringBuilder();
        for (NavbarItem item : items) {
            if (!settings.isNavbarItemShown(item)) {
                continue;
            }
            if (text.length() > 0) {
                text.append("  |  ");
            }
            text.append(context.getString(item.label));
        }
        TextView view = new TextView(context);
        view.setText(text);
        view.setSingleLine(true);
        view.setTextColor(context.getColor(R.color.text_primary));
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.chip_text));
        return view;
    }

    private static int iconOf(NavButton button) {
        switch (button) {
            case HOME:
                return R.drawable.ic_nav_home;
            case APPS:
                return R.drawable.ic_nav_apps;
            case SETTINGS:
                return R.drawable.ic_nav_settings;
            default:
                return R.drawable.ic_nav_wifi;
        }
    }

    /** Switches changed under Navbar Settings are picked up each time this opens. */
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && textGrid != null) {
            textGrid.refresh();
            refreshPreview();
            wireExits();
        }
    }

    private void wireExits() {
        textGrid.setExits(upExit, buttonGrid.preferredFocus());
        buttonGrid.setExits(textGrid.preferredFocus(), downExit);
    }

    @Override
    public View view() {
        return this;
    }

    @Override
    public View preferredFocus() {
        return textGrid.preferredFocus();
    }

    @Override
    public void setExits(View up, View down) {
        upExit = up;
        downExit = down;
        wireExits();
    }
}
