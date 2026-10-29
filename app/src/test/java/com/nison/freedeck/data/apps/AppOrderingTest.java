package com.nison.freedeck.data.apps;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AppOrderingTest {

    private static final Function<String, String> ID = name -> name;
    private static final List<String> ALL = Arrays.asList("Downloader", "HomeOnFire", "Netflix", "Spotify", "YouTube");

    @Test
    public void withoutFavoritesTheOrderIsUnchanged() {
        assertEquals(ALL, AppOrdering.favoritesFirst(ALL, Collections.<String>emptyList(), ID));
    }

    @Test
    public void favoritesComeFirstInTheOrderTheyWereAdded() {
        List<String> result = AppOrdering.favoritesFirst(ALL, Arrays.asList("YouTube", "Netflix"), ID);
        assertEquals(Arrays.asList("YouTube", "Netflix", "Downloader", "HomeOnFire", "Spotify"), result);
    }

    @Test
    public void theRestKeepTheirOriginalOrder() {
        List<String> result = AppOrdering.favoritesFirst(ALL, Collections.singletonList("Spotify"), ID);
        assertEquals(Arrays.asList("Spotify", "Downloader", "HomeOnFire", "Netflix", "YouTube"), result);
    }

    @Test
    public void aFavoriteThatIsNotInstalledIsSkipped() {
        List<String> result = AppOrdering.favoritesFirst(ALL, Arrays.asList("Gone", "Netflix"), ID);
        assertEquals(Arrays.asList("Netflix", "Downloader", "HomeOnFire", "Spotify", "YouTube"), result);
    }

    @Test
    public void everyAppAppearsExactlyOnce() {
        List<String> result = AppOrdering.favoritesFirst(ALL, Arrays.asList("Netflix", "Spotify", "Netflix"), ID);
        assertEquals(ALL.size(), result.size());
    }

    @Test
    public void favoritesOnlyKeepsJustTheFavoritesInOrder() {
        List<String> result = AppOrdering.favoritesOnly(ALL, Arrays.asList("YouTube", "Downloader"), ID);
        assertEquals(Arrays.asList("YouTube", "Downloader"), result);
    }

    @Test
    public void favoritesOnlyIsEmptyWithoutFavorites() {
        assertEquals(Collections.emptyList(), AppOrdering.favoritesOnly(ALL, Collections.<String>emptyList(), ID));
    }

    @Test
    public void favoritesOnlySkipsAppsThatAreNotInstalled() {
        assertEquals(Collections.singletonList("Netflix"),
                AppOrdering.favoritesOnly(ALL, Arrays.asList("Gone", "Netflix"), ID));
    }

    @Test
    public void emptyAppListGivesEmptyResults() {
        assertEquals(Collections.emptyList(), AppOrdering.favoritesFirst(Collections.<String>emptyList(), Arrays.asList("Netflix"), ID));
    }
}
