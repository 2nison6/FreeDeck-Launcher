package com.nison.freedeck.system.network;

import androidx.annotation.StringRes;

import com.nison.freedeck.R;

/** Human-friendly grading of a Wi-Fi signal strength. */
public enum SignalQuality {
    EXCELLENT(R.string.signal_excellent, Severity.GOOD),
    GOOD(R.string.signal_good, Severity.GOOD),
    FAIR(R.string.signal_fair, Severity.WARNING),
    WEAK(R.string.signal_weak, Severity.CRITICAL);

    @StringRes
    public final int label;
    public final Severity severity;

    SignalQuality(@StringRes int label, Severity severity) {
        this.label = label;
        this.severity = severity;
    }

    public static SignalQuality fromDbm(int dbm) {
        if (dbm >= -55) {
            return EXCELLENT;
        }
        if (dbm >= -65) {
            return GOOD;
        }
        if (dbm > -75) {
            return FAIR;
        }
        return WEAK;
    }
}
