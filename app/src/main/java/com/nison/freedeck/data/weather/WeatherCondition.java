package com.nison.freedeck.data.weather;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** Display category for a WMO weather code, with its icon and description. */
public enum WeatherCondition {
    CLEAR_DAY(R.drawable.ic_weather_clear_day, R.string.weather_clear),
    CLEAR_NIGHT(R.drawable.ic_weather_clear_night, R.string.weather_clear),
    PARTLY_CLOUDY(R.drawable.ic_weather_partly_cloudy, R.string.weather_partly_cloudy),
    CLOUDY(R.drawable.ic_weather_cloudy, R.string.weather_cloudy),
    FOG(R.drawable.ic_weather_fog, R.string.weather_fog),
    RAIN(R.drawable.ic_weather_rain, R.string.weather_rain),
    SNOW(R.drawable.ic_weather_snow, R.string.weather_snow),
    THUNDERSTORM(R.drawable.ic_weather_thunderstorm, R.string.weather_thunderstorm);

    @DrawableRes
    public final int icon;
    @StringRes
    public final int description;

    WeatherCondition(@DrawableRes int icon, @StringRes int description) {
        this.icon = icon;
        this.description = description;
    }

    /** Maps a WMO weather interpretation code (https://open-meteo.com/en/docs) to a condition. */
    public static WeatherCondition fromWmoCode(int code, boolean isDay) {
        if (code == 0) {
            return isDay ? CLEAR_DAY : CLEAR_NIGHT;
        }
        if (code <= 2) {
            return PARTLY_CLOUDY;
        }
        if (code == 3) {
            return CLOUDY;
        }
        if (code == 45 || code == 48) {
            return FOG;
        }
        if (code >= 95) {
            return THUNDERSTORM;
        }
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            return SNOW;
        }
        return RAIN;
    }
}
