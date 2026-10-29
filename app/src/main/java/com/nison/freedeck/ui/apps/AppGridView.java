package com.nison.freedeck.ui.apps;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;

import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nison.freedeck.R;
import com.nison.freedeck.data.apps.AppCatalog;
import com.nison.freedeck.data.apps.InstalledApp;
import com.nison.freedeck.system.AppLauncher;
import com.nison.freedeck.ui.common.GlowView;
import com.nison.freedeck.ui.sound.FocusSound;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * A grid of app cards (or, with {@code app:singleRow}, one row that scrolls sideways when there are more
 * cards than fit). Select opens the app, a long press shows its details with a favorites button,
 * the focused card grows a little, and an optional {@link GlowView} behind the grid takes on the colour of
 * the focused app (and goes out when focus leaves the grid).
 */
public class AppGridView extends RecyclerView {

    /** Which apps the grid shows. */
    public enum Source {
        /** Every app, favorites first. */
        ALL,
        /** Only the favorites. */
        FAVORITES
    }

    private final AppAdapter adapter;
    private final boolean singleRow;
    private int columns;
    /** Rows shown at once in a scrolling grid (see {@link #setGrid}); 0 leaves sizing to the layout. */
    private int visibleRows;
    private View prototype;
    private int prototypeWidth;
    private int heightAtBase;
    private int heightAtHalf;
    private final AppCatalog.Listener catalogListener = this::refresh;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener = this::onFocusChanged;

    private AppCatalog catalog;
    private Source source = Source.ALL;
    private GlowView glow;
    private FocusSound focusSound;
    private IntConsumer itemCountListener;
    @IdRes
    private int upTargetId = View.NO_ID;
    private String focusAfterRefresh;
    /** Whether a long press arranges the cards (instead of opening the app's details). */
    private boolean reorderEnabled;
    private int focusedPosition;

    public AppGridView(Context context) {
        this(context, null);
    }

    public AppGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.AppGridView);
        columns = attributes.getInt(R.styleable.AppGridView_columns,
                getResources().getInteger(R.integer.apps_grid_columns));
        singleRow = attributes.getBoolean(R.styleable.AppGridView_singleRow, false);
        boolean longPressEnabled = attributes.getBoolean(R.styleable.AppGridView_longPress, true);
        boolean favoriteBadgeEnabled = attributes.getBoolean(R.styleable.AppGridView_favoriteBadge, true);
        boolean addCardEnabled = attributes.getBoolean(R.styleable.AppGridView_addCard, false);
        reorderEnabled = attributes.getBoolean(R.styleable.AppGridView_reorder, false);
        attributes.recycle();
        setLayoutManager(singleRow
                ? new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                : new GridLayoutManager(context, columns));
        setItemAnimator(null);
        adapter = new AppAdapter(new AppAdapter.Listener() {
            @Override
            public void onAppClicked(InstalledApp app) {
                AppLauncher.launch(context, app);
            }

            @Override
            public void onAppLongClicked(InstalledApp app) {
                if (reorderEnabled) {
                    startReorder();
                } else {
                    showOptions(app);
                }
            }

            @Override
            public void onReorderMove(InstalledApp app, int delta) {
                if (catalog != null && catalog.moveFavorite(app.packageName, delta)) {
                    // The card slid to its new place: keep it in view and in focus.
                    String moved = app.packageName;
                    post(() -> adapter.focusPosition(adapter.positionOf(moved)));
                }
            }

            @Override
            public void onReorderDone() {
                stopReorder();
            }

            @Override
            public void onAddClicked() {
                new FavoritePickerDialog(context, catalog).show();
            }

            @Override
            public void onAddFocused() {
                focusedPosition = adapter.getItemCount() - 1;
                if (focusSound != null) {
                    focusSound.play();
                }
                if (glow != null) {
                    glow.hide(true);
                }
            }

            @Override
            public void onAppFocused(InstalledApp app, int position) {
                focusedPosition = position;
                if (focusSound != null) {
                    focusSound.play();
                }
                if (glow != null) {
                    glow.show(app.accentColor, true);
                }
            }
        }, packageName -> catalog != null && catalog.isFavorite(packageName), columns, singleRow, longPressEnabled, favoriteBadgeEnabled, addCardEnabled);
        setAdapter(adapter);
    }

    /**
     * @param upTargetId the view to focus when up is pressed on the first row
     * @param glow       the light behind the grid, or null for none
     * @param focusSound played as focus moves between cards, or null for silence
     */
    public void bind(AppCatalog catalog, Source source, @IdRes int upTargetId, GlowView glow, FocusSound focusSound) {
        this.catalog = catalog;
        this.source = source;
        this.upTargetId = upTargetId;
        this.glow = glow;
        this.focusSound = focusSound;
        adapter.setUpTarget(upTargetId);
        if (isAttachedToWindow()) {
            catalog.addListener(catalogListener);
        }
        refresh();
    }

    /**
     * Shows exactly {@code rows} rows of {@code columns} cards, centred in the space the grid has, with the
     * cards sized to fit; further rows scroll in one at a time as focus moves down.
     */
    public void setGrid(int columns, int rows) {
        if (singleRow) {
            return;
        }
        this.columns = columns;
        this.visibleRows = rows;
        ((GridLayoutManager) getLayoutManager()).setSpanCount(columns);
        adapter.setColumns(columns);
        requestLayout();
    }

    /**
     * In a single row, each card is a fraction of the visible width so that exactly {@code columns} fit.
     * Worked out while measuring, before the first card is laid out, so no card is ever drawn at the wrong size.
     */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (singleRow) {
            int available = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
            adapter.setItemWidth(available / columns);
        } else if (visibleRows > 0) {
            fitToRows(MeasureSpec.getSize(widthSpec), MeasureSpec.getSize(heightSpec));
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    /** Picks the card size so {@code visibleRows} rows fill the height, and pads the grid so they sit in the middle. */
    private void fitToRows(int width, int height) {
        int base = (width - 2 * getResources().getDimensionPixelSize(R.dimen.apps_grid_horizontal_padding)) / columns;
        if (base <= 0 || height <= 0) {
            return;
        }
        if (base != prototypeWidth) {
            prototypeWidth = base;
            heightAtBase = prototypeHeight(base);
            heightAtHalf = prototypeHeight(base / 2);
        }
        int itemWidth = GridSizing.itemWidth(base, heightAtBase, heightAtHalf, height / visibleRows);
        int itemHeight = GridSizing.heightAt(itemWidth, base, heightAtBase, heightAtHalf);
        int horizontal = Math.max(0, (width - columns * itemWidth) / 2);
        int vertical = Math.max(0, (height - visibleRows * itemHeight) / 2);
        if (getPaddingLeft() != horizontal || getPaddingTop() != vertical) {
            setPadding(horizontal, vertical, horizontal, vertical);
        }
    }

    /** How tall a card (picture and name) is at {@code width}, measured on a spare one. */
    private int prototypeHeight(int width) {
        if (prototype == null) {
            prototype = LayoutInflater.from(getContext()).inflate(R.layout.item_app, this, false);
            ((TextView) prototype.findViewById(R.id.app_label)).setText("Ag");
        }
        prototype.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return prototype.getMeasuredHeight();
    }

    /** Called with the number of cards whenever it changes, e.g. to show an "empty" message. */
    public void setOnItemCountChangedListener(IntConsumer listener) {
        itemCountListener = listener;
    }

    private void refresh() {
        if (catalog == null) {
            return;
        }
        List<InstalledApp> apps = source == Source.ALL ? catalog.allApps() : catalog.favoriteApps();
        adapter.setApps(apps);
        if (itemCountListener != null) {
            itemCountListener.accept(apps.size());
        }
        if (focusAfterRefresh != null) {
            restoreFocus(adapter.getItemCount());
        }
    }

    /** After the user changed a favorite the card may have moved or gone; put focus back where they were. */
    private void restoreFocus(int count) {
        int position = adapter.positionOf(focusAfterRefresh);
        focusAfterRefresh = null;
        if (position < 0) {
            position = Math.min(focusedPosition, count - 1);
        }
        if (position >= 0) {
            adapter.focusPosition(position);
            return;
        }
        View up = getRootView().findViewById(upTargetId);
        if (up != null) {
            up.requestFocus();
        }
    }

    /** Picks the cards up: they wiggle, and left / right move the focused one until OK or Back puts it down. */
    private void startReorder() {
        if (adapter.isReordering()) {
            return;
        }
        androidx.recyclerview.widget.DefaultItemAnimator animator = new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setMoveDuration(170L);
        animator.setSupportsChangeAnimations(false);
        setItemAnimator(animator);
        adapter.setReordering(true, true);
    }

    private void stopReorder() {
        if (!adapter.isReordering()) {
            return;
        }
        adapter.setReordering(false, false);
        setItemAnimator(null);
    }

    private void showOptions(InstalledApp app) {
        boolean favorite = catalog.isFavorite(app.packageName);
        new AppOptionsDialog(getContext(), app, favorite, () -> {
            focusAfterRefresh = app.packageName;
            catalog.toggleFavorite(app.packageName);
        }).show();
    }

    private void onFocusChanged(View oldFocus, View newFocus) {
        if (adapter.isReordering() && (newFocus == null || findContainingItemView(newFocus) == null)) {
            stopReorder(); // focus went elsewhere: put the cards down
        }
        if (glow != null && (newFocus == null || findContainingItemView(newFocus) == null)) {
            glow.hide(true);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (catalog != null) {
            catalog.addListener(catalogListener);
            refresh();
        }
        getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopReorder();
        if (catalog != null) {
            catalog.removeListener(catalogListener);
        }
        if (getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
        }
        super.onDetachedFromWindow();
    }
}
