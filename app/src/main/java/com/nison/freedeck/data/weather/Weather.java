package com.nison.freedeck.data.weather;

/** Current weather at a place. */
public final class Weather {

    public final double temperatureCelsius;
    public final WeatherCondition condition;

    public Weather(double temperatureCelsius, WeatherCondition condition) {
        this.temperatureCelsius = temperatureCelsius;
        this.condition = condition;
    }
}
