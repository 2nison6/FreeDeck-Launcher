package com.nison.freedeck.data.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FavoriteAppsTest {

    /** Keeps the value in memory, like preferences would on disk. */
    private static final class MemoryStorage implements FavoriteApps.Storage {
        private String value;

        MemoryStorage(String initial) {
            value = initial;
        }

        @Override
        public String read() {
            return value;
        }

        @Override
        public void write(String value) {
            this.value = value;
        }
    }

    private static FavoriteApps empty() {
        return new FavoriteApps(new MemoryStorage(""));
    }

    @Test
    public void startsEmpty() {
        FavoriteApps favorites = empty();
        assertEquals(Collections.emptyList(), favorites.packageNames());
        assertFalse(favorites.isFavorite("com.netflix.ninja"));
    }

    @Test
    public void togglingAnAppAddsIt() {
        FavoriteApps favorites = empty();
        assertTrue(favorites.toggle("com.netflix.ninja"));
        assertTrue(favorites.isFavorite("com.netflix.ninja"));
    }

    @Test
    public void togglingAgainRemovesIt() {
        FavoriteApps favorites = empty();
        favorites.toggle("com.netflix.ninja");
        assertFalse(favorites.toggle("com.netflix.ninja"));
        assertFalse(favorites.isFavorite("com.netflix.ninja"));
        assertEquals(Collections.emptyList(), favorites.packageNames());
    }

    @Test
    public void keepsTheOrderTheyWereAdded() {
        FavoriteApps favorites = empty();
        favorites.toggle("b.app");
        favorites.toggle("a.app");
        favorites.toggle("c.app");
        assertEquals(Arrays.asList("b.app", "a.app", "c.app"), favorites.packageNames());
    }

    @Test
    public void removingOneKeepsTheOthersInOrder() {
        FavoriteApps favorites = empty();
        favorites.toggle("a.app");
        favorites.toggle("b.app");
        favorites.toggle("c.app");
        favorites.toggle("b.app");
        assertEquals(Arrays.asList("a.app", "c.app"), favorites.packageNames());
    }

    @Test
    public void survivesBeingReloadedFromTheSameStorage() {
        MemoryStorage storage = new MemoryStorage("");
        new FavoriteApps(storage).toggle("com.spotify.tv.android");
        assertTrue(new FavoriteApps(storage).isFavorite("com.spotify.tv.android"));
    }

    @Test
    public void aPackageNameThatContainsAnotherIsNotConfusedWithIt() {
        FavoriteApps favorites = empty();
        favorites.toggle("com.example.app.pro");
        assertFalse(favorites.isFavorite("com.example.app"));
    }

    // ---- no limit ----

    @Test
    public void anyNumberOfAppsCanBeFavorites() {
        FavoriteApps favorites = empty();
        List<String> expected = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            assertTrue(favorites.toggle("app." + i));
            expected.add("app." + i);
        }
        assertEquals(expected, favorites.packageNames());
    }

    @Test
    public void aStoredListOfAnyLengthIsKeptWhole() {
        FavoriteApps favorites = new FavoriteApps(new MemoryStorage("a\nb\nc\nd\ne\nf\ng\nh\ni\nj"));
        assertEquals(10, favorites.packageNames().size());
    }

    @Test
    public void aRepeatedNameInStorageIsListedOnce() {
        FavoriteApps favorites = new FavoriteApps(new MemoryStorage("a\nb\na\nc"));
        assertEquals(Arrays.asList("a", "b", "c"), favorites.packageNames());
    }

    // ---- uninstalled apps ----

    @Test
    public void retainOnlyDropsAppsThatAreNoLongerInstalled() {
        FavoriteApps favorites = empty();
        favorites.toggle("a.app");
        favorites.toggle("gone.app");
        favorites.toggle("c.app");
        favorites.retainOnly(Arrays.asList("a.app", "c.app", "other.app"));
        assertEquals(Arrays.asList("a.app", "c.app"), favorites.packageNames());
    }

    @Test
    public void retainOnlyLeavesAnUnchangedListAlone() {
        MemoryStorage storage = new MemoryStorage("a\nb");
        new FavoriteApps(storage).retainOnly(Arrays.asList("a", "b", "c"));
        assertEquals("a\nb", storage.read());
    }

    @Test
    public void swappingTwoFavoritesExchangesTheirPlacesAndKeepsTheRest() {
        FavoriteApps favorites = empty();
        for (String name : Arrays.asList("a", "b", "c", "d")) {
            favorites.toggle(name);
        }
        assertTrue(favorites.swap("b", "c"));
        assertEquals(Arrays.asList("a", "c", "b", "d"), favorites.packageNames());
        assertTrue(favorites.swap("d", "a"));
        assertEquals(Arrays.asList("d", "c", "b", "a"), favorites.packageNames());
    }

    @Test
    public void swappingWithANonFavoriteOrItselfChangesNothing() {
        FavoriteApps favorites = empty();
        favorites.toggle("a");
        favorites.toggle("b");
        assertFalse(favorites.swap("a", "zzz"));
        assertFalse(favorites.swap("a", "a"));
        assertEquals(Arrays.asList("a", "b"), favorites.packageNames());
    }

    @Test
    public void anArrangedOrderIsKeptAcrossRestarts() {
        MemoryStorage storage = new MemoryStorage("");
        FavoriteApps favorites = new FavoriteApps(storage);
        favorites.toggle("a");
        favorites.toggle("b");
        favorites.swap("a", "b");
        assertEquals(Arrays.asList("b", "a"), new FavoriteApps(storage).packageNames());
    }
}
