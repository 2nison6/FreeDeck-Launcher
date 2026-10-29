package com.nison.freedeck.ui.apps;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.AppCatalog;
import com.nison.freedeck.data.apps.InstalledApp;

import java.util.List;

/**
 * Every installed app as a card, two across and three rows showing, the rest scrolling in a row at a time.
 * A card carries a plus (not a favorite yet) or a minus (already one); selecting it does that. The popup stays
 * open so several can be changed, and Back closes it.
 */
class FavoritePickerDialog extends Dialog {

    private static final int COLUMNS = 2;
    private static final int VISIBLE_ROWS = 3;
    private static final float FOCUSED_SCALE = 1.08f;
    private static final long FOCUS_ANIMATION_MS = 120L;
    private static final float FOCUSED_LIFT = 8f;

    private final AppCatalog catalog;
    private GridLayoutManager layoutManager;
    /** The first of the three rows showing. */
    private int firstRow;

    FavoritePickerDialog(Context context, AppCatalog catalog) {
        super(context, R.style.OptionsDialog);
        this.catalog = catalog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.nison.freedeck.ui.common.TextStyler.install(getWindow().getLayoutInflater(), getContext(),
                com.nison.freedeck.data.settings.AppSettings.from(getContext()));
        setContentView(R.layout.dialog_favorite_picker);
        RecyclerView list = findViewById(R.id.picker_list);
        layoutManager = new GridLayoutManager(getContext(), COLUMNS);
        list.setLayoutManager(layoutManager);
        list.setItemAnimator(null);
        // Exactly three rows tall (measured on a spare card), so a fourth never peeks in and none is cut.
        ViewGroup.LayoutParams params = list.getLayoutParams();
        params.height = cardHeight(list) * VISIBLE_ROWS;
        list.setLayoutParams(params);
        list.setAdapter(new Adapter(list, catalog.installedApps()));
        moveFocusTo(list, 0);
    }

    /** How tall one card (picture, name and margin) is. */
    private int cardHeight(RecyclerView list) {
        View card = LayoutInflater.from(getContext()).inflate(R.layout.item_favorite_card, list, false);
        ((TextView) card.findViewById(R.id.pick_label)).setText("Ag");
        card.measure(View.MeasureSpec.makeMeasureSpec(
                        getContext().getResources().getDimensionPixelSize(R.dimen.picker_card_width), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return card.getMeasuredHeight();
    }

    /**
     * Focuses a card, scrolling by whole rows: just far enough that its row is the first or last of the three
     * shown, so rows always sit squarely in the window.
     */
    private void moveFocusTo(RecyclerView list, int position) {
        int row = position / COLUMNS;
        if (row < firstRow) {
            firstRow = row;
        } else if (row > firstRow + VISIBLE_ROWS - 1) {
            firstRow = row - VISIBLE_ROWS + 1;
        }
        layoutManager.scrollToPositionWithOffset(firstRow * COLUMNS, 0);
        RecyclerView.ViewHolder ready = list.findViewHolderForAdapterPosition(position);
        if (ready != null && !list.isLayoutRequested()) {
            ready.itemView.requestFocus();
            return;
        }
        list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                RecyclerView.ViewHolder laidOut = list.findViewHolderForAdapterPosition(position);
                if (laidOut == null) {
                    return;
                }
                if (list.getViewTreeObserver().isAlive()) {
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                laidOut.itemView.requestFocus();
            }
        });
    }

    /** Arrow keys in the popup: left and right read through the cards, up and down move a row, all wrapping. */
    private boolean onArrowKey(RecyclerView list, int position, int keyCode, KeyEvent event, int count) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        int target;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            target = GridNavigation.afterRight(position, count);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            target = GridNavigation.afterLeft(position, count);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            target = GridNavigation.afterDown(position, count, COLUMNS);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            target = GridNavigation.afterUpWrapping(position, count, COLUMNS);
        } else {
            return false;
        }
        if (target != GridNavigation.NONE) {
            moveFocusTo(list, target);
        }
        return true; // never left to Android's focus search, which could jump out of the popup
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.Holder> {

        private final RecyclerView list;
        private final List<InstalledApp> apps;

        Adapter(RecyclerView list, List<InstalledApp> apps) {
            this.list = list;
            this.apps = apps;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_favorite_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            InstalledApp app = apps.get(position);
            holder.image.setImageDrawable(app.cardImage);
            holder.image.setScaleType(app.hasBanner ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            int padding = app.hasBanner ? 0 : holder.itemView.getResources().getDimensionPixelSize(R.dimen.app_icon_padding) / 2;
            holder.image.setPadding(padding, padding, padding, padding);
            holder.label.setText(app.label);
            holder.badge.setImageResource(catalog.isFavorite(app.packageName) ? R.drawable.ic_remove : R.drawable.ic_add);
            holder.itemView.setOnClickListener(v -> {
                catalog.toggleFavorite(app.packageName);
                notifyItemChanged(holder.getBindingAdapterPosition());
            });
            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? FOCUSED_SCALE : 1f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_ANIMATION_MS).start();
                // Lifted above its neighbours so the grown card is never drawn underneath them.
                v.setTranslationZ(hasFocus ? FOCUSED_LIFT : 0f);
            });
            holder.itemView.setOnKeyListener((v, keyCode, event) ->
                    FavoritePickerDialog.this.onArrowKey(list, holder.getBindingAdapterPosition(), keyCode, event, apps.size()));
        }

        @Override
        public void onViewRecycled(@NonNull Holder holder) {
            holder.itemView.animate().cancel();
            holder.itemView.setScaleX(1f);
            holder.itemView.setScaleY(1f);
            holder.itemView.setTranslationZ(0f);
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final ImageView badge;
            final TextView label;

            Holder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.pick_image);
                badge = itemView.findViewById(R.id.pick_badge);
                label = itemView.findViewById(R.id.pick_label);
            }
        }
    }
}
