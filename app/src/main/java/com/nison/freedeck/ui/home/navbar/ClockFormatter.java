package com.nison.freedeck.ui.home.navbar;

import com.nison.freedeck.data.settings.ClockFormat;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Turns a time into the text for the navbar: the digits, and in 12-hour format the AM / PM that follows them. */
final class ClockFormatter {

    /** The clock's text in two parts, so the suffix can be drawn smaller than the digits. */
    static final class Parts {
        final String time;
        /** "AM" / "PM" in 12-hour format; empty in 24-hour format. */
        final String suffix;

        Parts(String time, String suffix) {
            this.time = time;
            this.suffix = suffix;
        }
    }

    private ClockFormatter() {
    }

    static Parts parts(LocalTime now, ClockFormat format, Locale locale) {
        String minutes = String.format(Locale.US, "%02d", now.getMinute());
        if (format == ClockFormat.HOURS_24) {
            return new Parts(String.format(Locale.US, "%02d:%s", now.getHour(), minutes), "");
        }
        int hour = now.getHour() % 12 == 0 ? 12 : now.getHour() % 12;
        String suffix = DateTimeFormatter.ofPattern("a", locale).format(now);
        return new Parts(hour + ":" + minutes, suffix);
    }
}
