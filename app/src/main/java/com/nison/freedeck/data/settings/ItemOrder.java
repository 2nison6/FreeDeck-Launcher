package com.nison.freedeck.data.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Saving, loading and changing the order of a fixed set of items. */
public final class ItemOrder {

    private ItemOrder() {
    }

    /**
     * The items in the saved order. Anything unknown in {@code saved} is ignored, and any item missing from it
     * (say, one added in a later version) is put at the end in its default position.
     */
    public static <T> List<T> parse(String saved, T[] defaults, Function<T, String> id) {
        List<T> result = new ArrayList<>();
        if (saved != null) {
            for (String part : saved.split(",")) {
                for (T item : defaults) {
                    if (id.apply(item).equals(part.trim()) && !result.contains(item)) {
                        result.add(item);
                    }
                }
            }
        }
        for (T item : defaults) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static <T> String format(List<T> items, Function<T, String> id) {
        StringBuilder text = new StringBuilder();
        for (T item : items) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(id.apply(item));
        }
        return text.toString();
    }

    /** Swaps the items at the two positions. @return false, changing nothing, if either is outside the list */
    public static <T> boolean swap(List<T> items, int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) {
            return false;
        }
        T moved = items.get(from);
        items.set(from, items.get(to));
        items.set(to, moved);
        return true;
    }
}
