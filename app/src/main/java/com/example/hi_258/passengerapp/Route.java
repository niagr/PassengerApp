package com.example.hi_258.passengerapp;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by hi-258 on 2/5/17.
 */

public class Route {
    public int id;
    public List<Location> coords;
    public Route(int id, List<Location> coords) {
        this.id = id;
        this.coords = coords;
    }
}
