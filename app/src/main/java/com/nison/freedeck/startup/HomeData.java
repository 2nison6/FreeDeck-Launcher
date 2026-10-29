package com.nison.freedeck.startup;

import com.nison.freedeck.data.location.Place;
import com.nison.freedeck.data.weather.Weather;

/** Everything the home screen needs at first paint. Either field is null if it couldn't be loaded. */
public final class HomeData {

    public final Place place;
    public final Weather weather;

    public HomeData(Place place, Weather weather) {
        this.place = place;
        this.weather = weather;
    }
}
