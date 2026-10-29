package com.nison.freedeck.ui.home.navbar;

/** Which dividers to draw between the pieces of a navbar row, given which of the pieces are shown. */
final class NavbarLayout {

    private NavbarLayout() {
    }

    /**
     * A divider sits just before each piece after the first, and is drawn only when that piece is shown and
     * something before it is shown too, so hiding a piece never leaves a divider at an end or two side by side.
     *
     * @return one entry per piece after the first: whether the divider before that piece is drawn
     */
    static boolean[] dividersBefore(boolean... shown) {
        boolean[] dividers = new boolean[Math.max(0, shown.length - 1)];
        boolean anyBefore = shown.length > 0 && shown[0];
        for (int i = 1; i < shown.length; i++) {
            dividers[i - 1] = shown[i] && anyBefore;
            anyBefore |= shown[i];
        }
        return dividers;
    }

    static boolean anyShown(boolean... shown) {
        for (boolean one : shown) {
            if (one) {
                return true;
            }
        }
        return false;
    }
}
