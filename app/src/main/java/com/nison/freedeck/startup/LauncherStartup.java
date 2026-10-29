package com.nison.freedeck.startup;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.nison.freedeck.data.location.LocationRepository;
import com.nison.freedeck.data.location.Place;
import com.nison.freedeck.data.location.PlaceCache;
import com.nison.freedeck.data.network.Retry;
import com.nison.freedeck.data.weather.Weather;
import com.nison.freedeck.data.weather.WeatherRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads the data the home screen needs while the loader plays, then reports back on the main thread.
 * <p>
 * A TV that has only just been switched on often has no network for the first seconds, so each lookup is retried
 * until it works or {@link #LOAD_BUDGET_MS} has passed; if the location still cannot be found, the last known one
 * is used. Whatever is still missing can be fetched later with {@link #refreshLater}.
 */
public class LauncherStartup {

    /** Called on the main thread with the loaded data. Data fields may be null on failure. */
    public interface Callback {
        void onStartupFinished(HomeData data);
    }

    /** How long the first load keeps trying while the network comes up. */
    private static final long LOAD_BUDGET_MS = 10_000L;
    private static final long RETRY_PAUSE_MS = 1_000L;
    /** The quiet retries after the home screen is already showing. */
    private static final long LATE_BUDGET_MS = 3 * 60_000L;
    private static final long LATE_PAUSE_MS = 5_000L;

    private final LocationRepository locationRepository = new LocationRepository();
    private final WeatherRepository weatherRepository = new WeatherRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final PlaceCache placeCache;

    private boolean finished;
    private boolean cancelled;

    public LauncherStartup(Context context) {
        placeCache = new PlaceCache(context);
    }

    public void start(Callback callback) {
        executor.execute(() -> {
            HomeData data = loadWithRetries();
            handler.post(() -> finish(callback, data));
        });
    }

    /**
     * Keeps trying in the background for what {@code have} is missing, calling back as soon as the location or the
     * weather turns up. Gives up after a few minutes.
     */
    public void refreshLater(HomeData have, Callback callback) {
        executor.execute(() -> {
            AtomicReference<HomeData> best = new AtomicReference<>(have);
            Retry.until(() -> {
                HomeData fetched = fetchOnce(best.get().place);
                HomeData merged = merge(best.get(), fetched);
                if (!sameContent(merged, best.get())) {
                    best.set(merged);
                    handler.post(() -> deliver(callback, merged));
                }
                return merged.place != null && merged.weather != null ? merged : null;
            }, LATE_BUDGET_MS, LATE_PAUSE_MS, Retry.SYSTEM);
        });
    }

    /** Stops pending work and drops any result. */
    public void cancel() {
        cancelled = true;
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }

    private HomeData loadWithRetries() {
        AtomicReference<HomeData> best = new AtomicReference<>(new HomeData(null, null));
        Retry.until(() -> {
            HomeData fetched = fetchOnce(best.get().place);
            best.set(merge(best.get(), fetched));
            return best.get().place != null && best.get().weather != null ? best.get() : null;
        }, LOAD_BUDGET_MS, RETRY_PAUSE_MS, Retry.SYSTEM);

        HomeData data = best.get();
        if (data.place != null) {
            placeCache.write(data.place);
            return data;
        }
        // No network yet: show the last known place rather than "Location Unavailable"; the weather comes later.
        return new HomeData(placeCache.read(), null);
    }

    /** One look-up of the location (or the known one) and the weather there. */
    private HomeData fetchOnce(Place known) {
        Place place = known != null ? known : locationRepository.fetch();
        Weather weather = place != null ? weatherRepository.fetch(place) : null;
        return new HomeData(place, weather);
    }

    /** Whatever each has, preferring the newer where both do. */
    private static HomeData merge(HomeData old, HomeData fresh) {
        return new HomeData(fresh.place != null ? fresh.place : old.place,
                fresh.weather != null ? fresh.weather : old.weather);
    }

    private static boolean sameContent(HomeData a, HomeData b) {
        return a.place == b.place && a.weather == b.weather;
    }

    private void finish(Callback callback, HomeData data) {
        if (finished || cancelled) {
            return;
        }
        finished = true;
        callback.onStartupFinished(data);
    }

    private void deliver(Callback callback, HomeData data) {
        if (!cancelled) {
            callback.onStartupFinished(data);
        }
    }
}
