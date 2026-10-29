package com.nison.freedeck.data.apps;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** The apps the user starred, in the order they were added or arranged. There is no limit on how many. */
public class FavoriteApps {

    /** Where the list is kept; lets the logic be tested without Android. */
    public interface Storage {
        String read();

        void write(String value);
    }

    private static final String PREFERENCES_NAME = "launcher";
    private static final String KEY = "favorite_apps";
    private static final String SEPARATOR = "\n";

    private final Storage storage;

    public FavoriteApps(Storage storage) {
        this.storage = storage;
    }

    public static FavoriteApps from(Context context) {
        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return new FavoriteApps(new Storage() {
            @Override
            public String read() {
                return preferences.getString(KEY, "");
            }

            @Override
            public void write(String value) {
                preferences.edit().putString(KEY, value).apply();
            }
        });
    }

    public List<String> packageNames() {
        String stored = storage.read();
        List<String> names = new ArrayList<>();
        if (stored != null && !stored.isEmpty()) {
            for (String name : stored.split(SEPARATOR)) {
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    public boolean isFavorite(String packageName) {
        return packageNames().contains(packageName);
    }

    /** Adds the app, or removes it if it is already a favorite. @return true if it is a favorite after the call. */
    public boolean toggle(String packageName) {
        List<String> names = packageNames();
        boolean nowFavorite = !names.remove(packageName);
        if (nowFavorite) {
            names.add(packageName);
        }
        write(names);
        return nowFavorite;
    }

    /** Swaps the places of two favorites. @return false, changing nothing, if either is not a favorite */
    public boolean swap(String first, String second) {
        List<String> names = packageNames();
        int a = names.indexOf(first);
        int b = names.indexOf(second);
        if (a < 0 || b < 0 || a == b) {
            return false;
        }
        names.set(a, second);
        names.set(b, first);
        write(names);
        return true;
    }

    /** Forgets favorites that are no longer installed. */
    public void retainOnly(Collection<String> installedPackages) {
        List<String> names = packageNames();
        if (names.retainAll(installedPackages)) {
            write(names);
        }
    }

    private void write(List<String> names) {
        storage.write(String.join(SEPARATOR, names));
    }
}
