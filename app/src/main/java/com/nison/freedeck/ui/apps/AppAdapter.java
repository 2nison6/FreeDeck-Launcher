package com.nison.freedeck.ui.apps;

import android.content.res.ColorStateList;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.InstalledApp;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** TV-style cards for a grid of apps. */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {

    /** Events from the cards. */
    public interface Listener {
        void onAppClicked(InstalledApp app);

        void onAppLongClicked(InstalledApp app);

        void onAppFocused(InstalledApp app, int position);

        /** The trailing "+" card was selected. */
        void onAddClicked();

        /** Focus reached the trailing "+" card. */
        void onAddFocused();

        /** While arranging: the held card should move one place ({@code delta} -1 left, +1 right). */
        void onReorderMove(InstalledApp app, int delta);

        /** While arranging: the user put the card down. */
        void onReorderDone();
    }

    private static final float FOCUSED_SCALE = 1.08f;
    private static final long FOCUS_ANIMATION_MS = 120L;
    private static final float WIGGLE_DEGREES = 2.2f;
    private static final long WIGGLE_MS = 105L;

    private final Listener listener;
    private final Predicate<String> isFavorite;
    private int columns;
    private final boolean singleRow;
    private final boolean longPressEnabled;
    private final boolean favoriteBadgeEnabled;
    private final boolean addCard;
    private int upTargetId = View.NO_ID;
    /** Pixel width of each card when the cards sit in one sideways-scrolling row; 0 lets the layout decide. */
    private int itemWidth;
    private List<InstalledApp> apps = new ArrayList<>();
    private RecyclerView recyclerView;
    /** Whether the cards are being arranged: they wiggle, and left / right move the focused one. */
    private boolean reordering;
    /** The press that started arranging is still held; its release must not count as "drop". */
    private boolean entryPressActive;

    public AppAdapter(Listener listener, Predicate<String> isFavorite, int columns, boolean singleRow,
                      boolean longPressEnabled, boolean favoriteBadgeEnabled, boolean addCard) {
        this.listener = listener;
        this.isFavorite = isFavorite;
        this.columns = columns;
        this.singleRow = singleRow;
        this.longPressEnabled = longPressEnabled;
        this.favoriteBadgeEnabled = favoriteBadgeEnabled;
        this.addCard = addCard;
    }

    /** The number of cards per row, which decides which cards are in the first row. */
    public void setColumns(int columns) {
        if (this.columns != columns) {
            this.columns = columns;
            notifyDataSetChanged();
        }
    }

    /** Where pressing up from the first row goes, such as a button in the nav bar. */
    public void setUpTarget(@IdRes int viewId) {
        upTargetId = viewId;
        notifyDataSetChanged();
    }

    /**
     * Sets the width of every card (used by the single-row layout, where nothing else sizes them).
     * Cards that were already drawn are re-bound afterwards, never from inside a measure or layout pass.
     */
    public void setItemWidth(int widthPx) {
        if (widthPx == itemWidth) {
            return;
        }
        boolean hadWidth = itemWidth > 0;
        itemWidth = widthPx;
        if (hadWidth && recyclerView != null) {
            recyclerView.post(this::notifyDataSetChanged);
        }
    }

    public void setApps(List<InstalledApp> updated) {
        List<InstalledApp> old = apps;
        apps = new ArrayList<>(updated);
        if (reordering && sameApps(old, apps)) {
            // Only the order changed: let the cards slide to their new places instead of redrawing them all.
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return old.size();
                }

                @Override
                public int getNewListSize() {
                    return apps.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPosition, int newPosition) {
                    return old.get(oldPosition).packageName.equals(apps.get(newPosition).packageName);
                }

                @Override
                public boolean areContentsTheSame(int oldPosition, int newPosition) {
                    return true;
                }
            }, true).dispatchUpdatesTo(this);
        } else {
            notifyDataSetChanged();
        }
    }

    private static boolean sameApps(List<InstalledApp> first, List<InstalledApp> second) {
        if (first.size() != second.size()) {
            return false;
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        for (InstalledApp app : first) {
            names.add(app.packageName);
        }
        for (InstalledApp app : second) {
            if (!names.contains(app.packageName)) {
                return false;
            }
        }
        return true;
    }

    public boolean isReordering() {
        return reordering;
    }

    /** Starts or ends arranging. Cards on screen start or stop wiggling straight away, without being redrawn. */
    public void setReordering(boolean on, boolean startedByHeldPress) {
        reordering = on;
        entryPressActive = on && startedByHeldPress;
        if (recyclerView == null) {
            return;
        }
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder instanceof Holder) {
                ((Holder) holder).setWiggling(on && holder.getBindingAdapterPosition() < apps.size());
            }
        }
    }

    /** @return the app's position, or -1 if it isn't in the list */
    public int positionOf(String packageName) {
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).packageName.equals(packageName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        this.recyclerView = null;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (itemWidth > 0) {
            holder.itemView.getLayoutParams().width = itemWidth;
        }
        if (position >= apps.size()) {
            bindAddCard(holder, position);
            return;
        }
        InstalledApp app = apps.get(position);
        holder.bind(app, favoriteBadgeEnabled && isFavorite.test(app.packageName));
        holder.itemView.setNextFocusUpId(singleRow || position < columns ? upTargetId : View.NO_ID);
        holder.itemView.setOnClickListener(v -> listener.onAppClicked(app));
        if (longPressEnabled) {
            holder.itemView.setOnLongClickListener(v -> {
                listener.onAppLongClicked(app);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setLongClickable(false); // setting a listener, even null, turns this back on
        }
        holder.itemView.setOnKeyListener((v, keyCode, event) -> onCardKey(holder, keyCode, event));
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            holder.setFocused(hasFocus);
            if (hasFocus) {
                if (!singleRow) {
                    scrollIntoView(v);
                }
                listener.onAppFocused(app, holder.getBindingAdapterPosition());
            }
        });
        holder.setWiggling(reordering);
    }

    /** Left/right/down handling shared by every card, the "+" card included. */
    private boolean onCardKey(Holder holder, int keyCode, KeyEvent event) {
        if (reordering) {
            return onReorderKey(holder, keyCode, event);
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        int current = holder.getBindingAdapterPosition();
        int target;
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            target = GridNavigation.afterRight(current, getItemCount());
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            target = GridNavigation.afterLeft(current, getItemCount());
        } else if (!singleRow && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            target = GridNavigation.afterDown(current, getItemCount(), columns);
        } else {
            return false;
        }
        // Always consumed: left and right must never fall through to Android's focus search, which could
        // pick a nav bar button as "the nearest thing to the side".
        if (target != GridNavigation.NONE) {
            focusPosition(target);
        }
        return true;
    }

    /**
     * While arranging, every key is handled here: left / right move the held card, OK or Back puts it down, and
     * up / down do nothing (focus must not wander off while a card is picked up).
     */
    private boolean onReorderKey(Holder holder, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                int position = holder.getBindingAdapterPosition();
                if (event.getAction() == KeyEvent.ACTION_DOWN && position >= 0 && position < apps.size()) {
                    listener.onReorderMove(apps.get(position), keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (entryPressActive) {
                        entryPressActive = false; // the release of the press that picked the card up
                    } else {
                        listener.onReorderDone();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    listener.onReorderDone();
                }
                return true;
            default:
                return false;
        }
    }

    private void bindAddCard(Holder holder, int position) {
        holder.setWiggling(false);
        holder.bindAdd();
        holder.itemView.setNextFocusUpId(singleRow || position < columns ? upTargetId : View.NO_ID);
        holder.itemView.setOnClickListener(v -> listener.onAddClicked());
        holder.itemView.setOnLongClickListener(null);
        holder.itemView.setLongClickable(false);
        holder.itemView.setOnKeyListener((v, keyCode, event) -> onCardKey(holder, keyCode, event));
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            holder.setFocused(hasFocus);
            if (hasFocus) {
                if (!singleRow) {
                    scrollIntoView(v);
                }
                listener.onAddFocused();
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        holder.resetFocusLook();
    }

    @Override
    public int getItemCount() {
        return apps.size() + (addCard ? 1 : 0);
    }

    /** Focuses a card, scrolling to it and waiting for the next layout if it isn't on screen yet. */
    public void focusPosition(int position) {
        if (recyclerView == null || position < 0 || position >= getItemCount()) {
            return;
        }
        RecyclerView list = recyclerView;
        RecyclerView.ViewHolder target = list.findViewHolderForAdapterPosition(position);
        if (target != null && !list.isLayoutRequested() && isFullyVisible(list, target.itemView)) {
            target.itemView.requestFocus();
            return;
        }
        list.scrollToPosition(position);
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

    /** Scrolls a focused card up or down until it lies inside the visible rows, however it got focus. */
    private void scrollIntoView(View card) {
        if (recyclerView == null) {
            return;
        }
        int above = card.getTop() - recyclerView.getPaddingTop();
        int below = card.getBottom() - (recyclerView.getHeight() - recyclerView.getPaddingBottom());
        if (above < 0) {
            recyclerView.scrollBy(0, above);
        } else if (below > 0) {
            recyclerView.scrollBy(0, below);
        }
    }

    /** Whether the card lies inside the part of the list that is on screen (its padding is clipped away). */
    private static boolean isFullyVisible(RecyclerView list, View card) {
        return card.getTop() >= list.getPaddingTop()
                && card.getBottom() <= list.getHeight() - list.getPaddingBottom()
                && card.getLeft() >= list.getPaddingLeft()
                && card.getRight() <= list.getWidth() - list.getPaddingRight();
    }

    static class Holder extends RecyclerView.ViewHolder {

        private final ImageView image;
        private final View favoriteBadge;
        private final ImageView favoriteHeart;
        private final TextView label;
        private final int iconPadding;
        private android.animation.ObjectAnimator wiggle;

        Holder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.app_image);
            favoriteBadge = itemView.findViewById(R.id.app_favorite);
            favoriteHeart = itemView.findViewById(R.id.app_favorite_heart);
            label = itemView.findViewById(R.id.app_label);
            iconPadding = itemView.getResources().getDimensionPixelSize(R.dimen.app_icon_padding);
        }

        /** The trailing card: a plus on the same card surface, with no favorite ribbon. */
        void bindAdd() {
            image.setImageResource(R.drawable.ic_add);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int padding = itemView.getResources().getDimensionPixelSize(R.dimen.add_card_icon_padding);
            image.setPadding(padding, padding, padding, padding);
            label.setText(" "); // no name shown, but the same height as the other cards
            itemView.setContentDescription(itemView.getResources().getString(R.string.home_add_favorite));
            favoriteBadge.setVisibility(View.GONE);
        }

        void bind(InstalledApp app, boolean showFavoriteBadge) {
            image.setImageDrawable(app.cardImage);
            // Banners fill the card; a plain icon sits centred on it with some breathing room.
            image.setScaleType(app.hasBanner ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            int padding = app.hasBanner ? 0 : iconPadding;
            image.setPadding(padding, padding, padding, padding);
            label.setText(app.label);
            itemView.setContentDescription(null);
            favoriteHeart.setImageTintList(ColorStateList.valueOf(app.accentColor));
            favoriteBadge.setVisibility(showFavoriteBadge ? View.VISIBLE : View.GONE);
        }

        /**
         * Grows the whole item a little (card and name together); the name appears via the label's colour.
         * The gaps between cards are wider than the growth, so neighbours never overlap.
         */
        void setFocused(boolean focused) {
            float scale = focused ? FOCUSED_SCALE : 1f;
            itemView.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_ANIMATION_MS).start();
        }

        /** Rocks the card a degree or two back and forth, each at its own point in the swing, like arranging icons. */
        void setWiggling(boolean on) {
            if (!on) {
                if (wiggle != null) {
                    wiggle.cancel();
                    wiggle = null;
                    itemView.setRotation(0f);
                }
                return;
            }
            if (wiggle != null) {
                return;
            }
            wiggle = android.animation.ObjectAnimator.ofFloat(itemView, View.ROTATION, -WIGGLE_DEGREES, WIGGLE_DEGREES);
            wiggle.setDuration(WIGGLE_MS);
            wiggle.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            wiggle.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            wiggle.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            wiggle.start();
            wiggle.setCurrentPlayTime((long) (Math.random() * WIGGLE_MS * 2));
        }

        void resetFocusLook() {
            setWiggling(false);
            itemView.animate().cancel();
            itemView.setScaleX(1f);
            itemView.setScaleY(1f);
        }
    }
}
