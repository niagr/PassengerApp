package com.example.hi_258.passengerapp;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private List<LatLng> mMockCoords;
    private int currMarkerCoordIndex = 0;

    private Button mNextButton;
    private Marker mMarker;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        List<LatLng> coordList = getMockCoordinates();
        if (coordList != null) {
            mMockCoords = coordList;
        } else {
            Log.d("nish", "Could not get mock coordinates");
        }
        mapFragment.getMapAsync(this);

        mNextButton = (Button)findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMarker.setPosition(mMockCoords.get(++currMarkerCoordIndex));
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        PolylineOptions path = new PolylineOptions().addAll(mMockCoords);
        mMarker = mMap.addMarker(new MarkerOptions().position(mMockCoords.get(currMarkerCoordIndex)));
//        mMap.addMarker(new MarkerOptions().position(mMockCoords.get(1)));
//        mMap.addMarker(new MarkerOptions().position(mMockCoords.get(2)));
        mMap.addPolyline(path);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mMockCoords.get(0), 12));
    }

    // Returns null if fails
    List<LatLng> getMockCoordinates () {
        InputStream is = getResources().openRawResource(R.raw.coordinates);
        byte[] data;
        try {
            int size = is.available();
            data = new byte[size];
            is.read(data);
            is.close();
        } catch (IOException e) {
            Log.d("nish", "Could not get mock data");
            return null;
        }
        String jsonStr = new String(data);

        JSONArray jsonArr;
        List<LatLng> coordList = new ArrayList<LatLng>();
        try {
            jsonArr = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONArray coords = jsonArr.getJSONArray(i);
                double latitude = Double.parseDouble(coords.getString(0));
                double longitude = Double.parseDouble(coords.getString(1));
                coordList.add(new LatLng(latitude, longitude));
            }
            return removeAnomalousCoordinates(coordList);
        } catch (JSONException | NumberFormatException e) {
            Log.d("nish", "Could not parse JSON object: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get rid of anomalous location
     */
    List<LatLng> removeAnomalousCoordinates (List<LatLng> coordList) {
        List<LatLng> filteredCoordList = new ArrayList<LatLng>();
        LatLng lastCoord = coordList.get(0);
        int l = 0;
        for (int i = 1; i < coordList.size(); i++) {
            LatLng currCoord = coordList.get(i);
            double dist = distance(lastCoord.latitude, currCoord.latitude, lastCoord.longitude, lastCoord.longitude, 0, 0);
            Log.d("nish", i + " " + l + " DIST: " + dist + " " + lastCoord.toString() + " - " + currCoord.toString());
            if (dist < 500) {
                filteredCoordList.add(currCoord);
                lastCoord = currCoord;
                l = i;
            }
        }
        return filteredCoordList;
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
