package com.nison.freedeck.data.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** How the app list is arranged around the favorites. */
public final class AppOrdering {

    private AppOrdering() {
    }

    /** Just the favorites, in the order they were added. Favorites that aren't in {@code all} are skipped. */
    public static <T> List<T> favoritesOnly(List<T> all, List<String> favoriteOrder, Function<T, String> id) {
        Map<String, T> byId = new HashMap<>();
        for (T item : all) {
            byId.put(id.apply(item), item);
        }
        List<T> favorites = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (String name : favoriteOrder) {
            T item = byId.get(name);
            if (item != null && added.add(name)) { // a repeated name would otherwise show the app twice
                favorites.add(item);
            }
        }
        return favorites;
    }

    /** The favorites first (in the order they were added), then everything else in its original order. */
    public static <T> List<T> favoritesFirst(List<T> all, List<String> favoriteOrder, Function<T, String> id) {
        List<T> ordered = favoritesOnly(all, favoriteOrder, id);
        Set<String> favoriteIds = new HashSet<>();
        for (T item : ordered) {
            favoriteIds.add(id.apply(item));
        }
        for (T item : all) {
            if (!favoriteIds.contains(id.apply(item))) {
                ordered.add(item);
            }
        }
        return ordered;
    }
}
