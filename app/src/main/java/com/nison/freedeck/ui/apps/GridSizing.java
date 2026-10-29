package com.nison.freedeck.ui.apps;

/**
 * Sizes the cards of a grid that must show an exact number of rows in the space it has. A card's height grows
 * in step with its width (the picture keeps its shape; the name below it is fixed), so from the height measured
 * at two widths the width that makes the rows fill the space can be worked out.
 */
final class GridSizing {

    private GridSizing() {
    }

    /**
     * @param baseWidth    the widest a card may be
     * @param heightAtBase a card's height at {@code baseWidth}
     * @param heightAtHalf a card's height at half of {@code baseWidth}
     * @param rowBudget    the height available for one row
     * @return {@code baseWidth}, or less when a row that wide would be taller than {@code rowBudget}
     */
    static int itemWidth(int baseWidth, int heightAtBase, int heightAtHalf, int rowBudget) {
        double slope = slope(baseWidth, heightAtBase, heightAtHalf);
        if (heightAtBase <= rowBudget || slope <= 0) {
            return baseWidth;
        }
        double width = baseWidth + (rowBudget - heightAtBase) / slope;
        return (int) Math.max(1, Math.floor(width));
    }

    /** A card's height at {@code width}, from the two measurements. */
    static int heightAt(int width, int baseWidth, int heightAtBase, int heightAtHalf) {
        return (int) Math.ceil(heightAtBase + slope(baseWidth, heightAtBase, heightAtHalf) * (width - baseWidth));
    }

    private static double slope(int baseWidth, int heightAtBase, int heightAtHalf) {
        return (heightAtBase - heightAtHalf) / (baseWidth / 2.0);
    }
}
