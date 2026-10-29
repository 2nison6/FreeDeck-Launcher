package com.nison.freedeck.data.weather;

import com.nison.freedeck.data.location.Place;
import com.nison.freedeck.data.network.HttpJson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

/** Fetches current weather from Open-Meteo (free, no API key). */
public class WeatherRepository {

    private static final String URL_FORMAT = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,is_day";

    /** Blocking; call from a background thread. Returns null if the weather can't be fetched. */
    public Weather fetch(Place place) {
        try {
            JSONObject current = HttpJson
                    .get(String.format(Locale.US, URL_FORMAT, place.latitude, place.longitude))
                    .getJSONObject("current");
            WeatherCondition condition = WeatherCondition.fromWmoCode(
                    current.getInt("weather_code"),
                    current.optInt("is_day", 1) == 1);
            return new Weather(current.getDouble("temperature_2m"), condition);
        } catch (IOException | JSONException e) {
            return null;
        }
    }
}
