package com.nison.freedeck.system.network;

/** The radio band a Wi-Fi connection is using. */
public enum WifiBand {
    GHZ_2_4("2.4 GHz"),
    GHZ_5("5 GHz"),
    GHZ_6("6 GHz"),
    UNKNOWN("");

    public final String label;

    WifiBand(String label) {
        this.label = label;
    }

    public static WifiBand fromMhz(int frequencyMhz) {
        if (frequencyMhz <= 0) {
            return UNKNOWN;
        }
        if (frequencyMhz < 3000) {
            return GHZ_2_4;
        }
        return frequencyMhz < 5925 ? GHZ_5 : GHZ_6;
    }
}
