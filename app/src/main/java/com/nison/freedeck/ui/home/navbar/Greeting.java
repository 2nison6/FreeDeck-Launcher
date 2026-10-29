package com.nison.freedeck.ui.home.navbar;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

import java.util.Calendar;

/** Picks the greeting for a time of day. */
public final class Greeting {

    private Greeting() {
    }

    @StringRes
    static int forHour(int hourOfDay) {
        if (hourOfDay >= 5 && hourOfDay < 12) {
            return R.string.greeting_morning;
        }
        if (hourOfDay >= 12 && hourOfDay < 17) {
            return R.string.greeting_afternoon;
        }
        if (hourOfDay >= 17 && hourOfDay < 21) {
            return R.string.greeting_evening;
        }
        return R.string.greeting_night;
    }

    @StringRes
    public static int now() {
        return forHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }
}
