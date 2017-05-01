package com.example.hi_258.passengerapp;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private List<Location> mMockCoords;
    private int currMarkerCoordIndex = 0;

    private Button mNextButton, mTrackButton;
    private TextView mLabelTime;
    private TextView mLabelCoords;
    private EditText mTxtRouteId;

    private Marker mMarker;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        List<Location> coordList = getMockCoordinates();
        if (coordList != null) {
            mMockCoords = coordList;
        } else {
            Log.d("nish", "Could not get mock coordinates");
        }
        mapFragment.getMapAsync(this);

        mLabelCoords = (TextView)findViewById(R.id.txtCoordinates);
        mLabelTime = (TextView)findViewById(R.id.txtTime);
        mTxtRouteId = (EditText)findViewById(R.id.txtRouteId);

        mNextButton = (Button)findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location loc = mMockCoords.get(++currMarkerCoordIndex);
                mMarker.setPosition(loc.coords);
                Log.d("nish", loc.time.toString());
                mLabelTime.setText(loc.time.toString());
                mLabelCoords.setText(loc.coords.latitude + " - " + loc.coords.longitude);
            }
        });

        mTrackButton = (Button)findViewById(R.id.trackButton);
        mTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String routeId = mTxtRouteId.getText().toString();
                if (routeId.length() == 0) {
                    Toast.makeText(MapsActivity.this, "Please enter route ID", Toast.LENGTH_LONG);
                    return;
                }
                getCurrentCoords(routeId);
            }
        });
    }


    void getCurrentCoords (final String routeId) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                String.format("http://ec2-54-202-217-53.us-west-2.compute.amazonaws.com:8080/current_coordinates/?route_id=%s", routeId),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("nish", "Got current coordinates" + response);
                        try {
                            JSONArray jsonArr = new JSONArray(response);
                            JSONObject jsonObj = jsonArr.getJSONObject(0);
                            double latitude = Double.parseDouble(jsonObj.getString("latitude"));
                            double longitude = Double.parseDouble(jsonObj.getString("longitude"));
                            mMarker.setPosition(new LatLng(latitude, longitude));
                        } catch (JSONException e) {
                            Log.d("nish", "Couldl not parse JSON");
                            return;
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("nish", "Failed to get current coordinates" + error.toString());
                    }
                }
        ) ;
//        {
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                Map<String, String> params = new HashMap<>();
//                params.put("route_id", routeId);
//                return params;
//            }
//        };
        Log.d("nish", "Making request: " + strReq.toString());
        queue.add(strReq);
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
        final List<LatLng> coords = new ArrayList<>();
        for (Location loc : mMockCoords) coords.add(loc.coords);
        PolylineOptions path = new PolylineOptions().addAll(coords);
        mMarker = mMap.addMarker(new MarkerOptions().position(mMockCoords.get(currMarkerCoordIndex).coords));
        mMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus_small));
//        mMap.addMarker(new MarkerOptions().position(mMockCoords.get(1)));
//        mMap.addMarker(new MarkerOptions().position(mMockCoords.get(2)));
        mMap.addPolyline(path);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mMockCoords.get(0).coords, 12));
    }

    // Returns null if fails
    List<Location> getMockCoordinates () {
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
        List<Location> locationList = new ArrayList<Location>();
        try {
            jsonArr = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONArray coords = jsonArr.getJSONArray(i);
                double latitude = Double.parseDouble(coords.getString(0));
                double longitude = Double.parseDouble(coords.getString(1));
                Time time = Time.valueOf("12:12:12");  // Time.valueOf(coords.getString(2));
                locationList.add(new Location(new LatLng(latitude, longitude), time));
            }
            return removeAnomalousCoordinates(locationList);
        } catch (JSONException | NumberFormatException e) {
            Log.d("nish", "Could not parse JSON object: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get rid of anomalous location
     */
    List<Location> removeAnomalousCoordinates (List<Location> locationList) {
        List<Location> filteredCoordList = new ArrayList<Location>();
        LatLng lastCoord = locationList.get(0).coords;
        int l = 0;
        for (int i = 1; i < locationList.size(); i++) {
            Location currLocation = locationList.get(i);
            LatLng currCoord = currLocation.coords;
            double dist = distance(lastCoord.latitude, currCoord.latitude, lastCoord.longitude, lastCoord.longitude, 0, 0);
            Log.d("nish", i + " " + l + " DIST: " + dist + " " + lastCoord.toString() + " - " + currCoord.toString());
            if (dist < 800) {
                filteredCoordList.add(currLocation);
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
