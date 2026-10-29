package com.nison.freedeck.ui.apps;

/**
 * Left/right focus rules for the app cards, which read like text: right moves to the next app and left to
 * the previous one, a full row continuing on the next row, and the two ends of the whole list join up.
 * <p>
 * The cards handle these two keys themselves rather than leave them to Android's focus search, which can
 * decide the nearest thing to the right or left is a button in the nav bar and jump focus out of the cards.
 */
public final class GridNavigation {

    public static final int NONE = -1;

    private GridNavigation() {
    }

    /** @return the position to focus after pressing right, or {@link #NONE} if {@code position} isn't a card */
    public static int afterRight(int position, int itemCount) {
        return isCard(position, itemCount) ? (position + 1) % itemCount : NONE;
    }

    /** @return the position to focus after pressing left, or {@link #NONE} if {@code position} isn't a card */
    public static int afterLeft(int position, int itemCount) {
        return isCard(position, itemCount) ? (position - 1 + itemCount) % itemCount : NONE;
    }

    /**
     * @return the position to focus after pressing down in a grid of {@code columns}: the card below, or from the
     * last row the card at the top of the same column; a last row too short for the column goes to the last card
     */
    public static int afterDown(int position, int itemCount, int columns) {
        if (!isCard(position, itemCount)) {
            return NONE;
        }
        int below = position + columns;
        if (below < itemCount) {
            return below;
        }
        boolean inLastRow = position / columns == (itemCount - 1) / columns;
        return inLastRow ? position % columns : itemCount - 1;
    }

    /**
     * @return the position to focus after pressing up where the grid has nothing above it to leave to: the card
     * above, or from the top row the card at the bottom of the same column (the last card when the last row is
     * too short to have one)
     */
    public static int afterUpWrapping(int position, int itemCount, int columns) {
        if (!isCard(position, itemCount)) {
            return NONE;
        }
        if (position >= columns) {
            return position - columns;
        }
        int bottom = (itemCount - 1) / columns * columns + position % columns;
        return Math.min(bottom, itemCount - 1);
    }

    private static boolean isCard(int position, int itemCount) {
        return position >= 0 && position < itemCount;
    }
}
