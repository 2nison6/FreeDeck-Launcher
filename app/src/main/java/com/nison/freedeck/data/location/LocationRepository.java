package com.nison.freedeck.data.location;

import com.nison.freedeck.data.network.HttpJson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/** Resolves the TV's approximate location from its public IP address (a TV has no GPS). */
public class LocationRepository {

    private static final String URL = "https://ipwho.is/";

    /** Blocking; call from a background thread. Returns null if the location can't be determined. */
    public Place fetch() {
        try {
            JSONObject json = HttpJson.get(URL);
            if (!json.optBoolean("success", false)) {
                return null;
            }
            return new Place(
                    json.optString("city"),
                    json.optString("region"),
                    json.getDouble("latitude"),
                    json.getDouble("longitude"));
        } catch (IOException | JSONException e) {
            return null;
        }
    }
}
