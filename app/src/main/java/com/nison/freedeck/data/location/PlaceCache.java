package com.nison.freedeck.data.location;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * The last place that was successfully looked up. A TV that has just been switched on often has no network for
 * the first seconds, so this lets the screen show where it is straight away instead of "Location Unavailable".
 */
public class PlaceCache {

    private static final String PREFERENCES_NAME = "place_cache";

    private final SharedPreferences preferences;

    public PlaceCache(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public Place read() {
        if (!preferences.contains("latitude")) {
            return null;
        }
        return new Place(
                preferences.getString("city", ""),
                preferences.getString("region", ""),
                Double.longBitsToDouble(preferences.getLong("latitude", 0L)),
                Double.longBitsToDouble(preferences.getLong("longitude", 0L)));
    }

    public void write(Place place) {
        preferences.edit()
                .putString("city", place.city)
                .putString("region", place.region)
                .putLong("latitude", Double.doubleToRawLongBits(place.latitude))
                .putLong("longitude", Double.doubleToRawLongBits(place.longitude))
                .apply();
    }
}
