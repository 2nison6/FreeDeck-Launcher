package com.nison.freedeck.data.location;

/** An approximate geographic place. */
public final class Place {

    public final String city;
    public final String region;
    public final double latitude;
    public final double longitude;

    public Place(String city, String region, double latitude, double longitude) {
        this.city = city;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
