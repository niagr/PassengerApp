package com.example.hi_258.passengerapp;

import com.google.android.gms.maps.model.LatLng;

import java.sql.Time;

/**
 * Created by hi-258 on 30/4/17.
 */

public class Location {
    public LatLng coords;
    public Time time;
    public Location(LatLng coords, Time time) {
        this.coords = coords;
        this.time = time;
    }
    @Override
    public String toString() {
        return this.coords.toString();
    }
}
