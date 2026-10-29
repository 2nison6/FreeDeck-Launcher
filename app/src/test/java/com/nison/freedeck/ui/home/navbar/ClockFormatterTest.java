package com.nison.freedeck.ui.home.navbar;

import static org.junit.Assert.assertEquals;

import com.nison.freedeck.data.settings.ClockFormat;

import org.junit.Test;

import java.time.LocalTime;
import java.util.Locale;

public class ClockFormatterTest {

    private static ClockFormatter.Parts of(int hour, int minute, ClockFormat format) {
        return ClockFormatter.parts(LocalTime.of(hour, minute), format, Locale.US);
    }

    @Test
    public void afternoonInTwelveHourFormat() {
        ClockFormatter.Parts parts = of(17, 5, ClockFormat.HOURS_12);
        assertEquals("5:05", parts.time);
        assertEquals("PM", parts.suffix);
    }

    @Test
    public void justAfterMidnightIsTwelveAm() {
        ClockFormatter.Parts parts = of(0, 7, ClockFormat.HOURS_12);
        assertEquals("12:07", parts.time);
        assertEquals("AM", parts.suffix);
    }

    @Test
    public void noonIsTwelvePm() {
        ClockFormatter.Parts parts = of(12, 0, ClockFormat.HOURS_12);
        assertEquals("12:00", parts.time);
        assertEquals("PM", parts.suffix);
    }

    @Test
    public void morningHasNoLeadingZeroInTwelveHourFormat() {
        assertEquals("9:30", of(9, 30, ClockFormat.HOURS_12).time);
        assertEquals("AM", of(9, 30, ClockFormat.HOURS_12).suffix);
    }

    @Test
    public void twentyFourHourFormatPadsAndHasNoSuffix() {
        ClockFormatter.Parts parts = of(9, 3, ClockFormat.HOURS_24);
        assertEquals("09:03", parts.time);
        assertEquals("", parts.suffix);
        assertEquals("17:05", of(17, 5, ClockFormat.HOURS_24).time);
        assertEquals("00:00", of(0, 0, ClockFormat.HOURS_24).time);
    }
}
