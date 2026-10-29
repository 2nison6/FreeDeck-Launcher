package com.nison.freedeck.data.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The installed apps and which of them are favorites, shared by every screen that shows apps.
 * Reloads by itself when apps are installed, updated or removed. Use from the main thread.
 */
public class AppCatalog {

    /** Told, on the main thread, whenever the apps or the favorites change. */
    public interface Listener {
        void onCatalogChanged();
    }

    private final Context context;
    private final AppRepository repository;
    private final FavoriteApps favorites;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload();
        }
    };

    private List<InstalledApp> installed = Collections.emptyList();
    private ExecutorService executor;

    public AppCatalog(Context context) {
        this.context = context.getApplicationContext();
        this.repository = new AppRepository(this.context);
        this.favorites = FavoriteApps.from(this.context);
    }

    public void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadExecutor();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        context.registerReceiver(packageReceiver, filter);
        reload();
    }

    public void stop() {
        if (executor == null) {
            return;
        }
        context.unregisterReceiver(packageReceiver);
        executor.shutdownNow();
        executor = null;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** Every app, favorites first. */
    public List<InstalledApp> allApps() {
        return AppOrdering.favoritesFirst(installed, favorites.packageNames(), app -> app.packageName);
    }

    /** Every real installed app by name, favorites not moved first; for picking which to favorite. */
    public List<InstalledApp> installedApps() {
        return new ArrayList<>(installed);
    }

    /** Only the favorites, in the order they were added. */
    public List<InstalledApp> favoriteApps() {
        return AppOrdering.favoritesOnly(installed, favorites.packageNames(), app -> app.packageName);
    }

    public boolean isFavorite(String packageName) {
        return favorites.isFavorite(packageName);
    }

    /** Adds or removes the app as a favorite. @return true if it is a favorite afterwards. */
    public boolean toggleFavorite(String packageName) {
        boolean nowFavorite = favorites.toggle(packageName);
        notifyListeners();
        return nowFavorite;
    }

    /**
     * Moves a favorite one place left ({@code delta} -1) or right (+1) among the favorites shown.
     * @return false if it is already at that end, or is not a favorite
     */
    public boolean moveFavorite(String packageName, int delta) {
        List<InstalledApp> shown = favoriteApps();
        int from = -1;
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).packageName.equals(packageName)) {
                from = i;
            }
        }
        int to = from + delta;
        if (from < 0 || to < 0 || to >= shown.size()) {
            return false;
        }
        if (!favorites.swap(shown.get(from).packageName, shown.get(to).packageName)) {
            return false;
        }
        notifyListeners();
        return true;
    }

    private void reload() {
        ExecutorService running = executor;
        if (running == null) {
            return;
        }
        running.execute(() -> {
            List<InstalledApp> apps = repository.load();
            mainHandler.post(() -> onLoaded(running, apps));
        });
    }

    private void onLoaded(ExecutorService source, List<InstalledApp> apps) {
        if (source != executor) {
            return; // stopped (or restarted) while loading
        }
        installed = apps;
        // An empty result more likely means the lookup failed than that every app was removed.
        if (!apps.isEmpty()) {
            Set<String> packages = new HashSet<>();
            for (InstalledApp app : apps) {
                packages.add(app.packageName);
            }
            favorites.retainOnly(packages);
        }
        notifyListeners();
    }

    private void notifyListeners() {
        for (Listener listener : new ArrayList<>(listeners)) {
            listener.onCatalogChanged();
        }
    }
}
