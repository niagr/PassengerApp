package com.example.hi_258.passengerapp;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.List;
import android.os.Handler;

import static java.lang.Math.abs;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private List<Location> mMockCoords;
    private int currMarkerCoordIndex = 0;

    private Button mNextButton, mTrackButton;
    private TextView mTxtEta;
    private EditText mTxtRouteId;

    private SupportMapFragment mMapFragment;

    private Spinner mRoutesDropDown, mStopsDropDown;

    private List<Location> mRouteCoords = new ArrayList<>();
    private List<Location> mStops = new ArrayList<>();

    private Location mSelectedStop;

    private Marker mBusMarker, mStopMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        List<Location> coordList = getMockCoordinates();
        if (coordList != null) {
            mMockCoords = coordList;
        } else {
            Log.d("nish", "Could not get mock coordinates");
        }

        mRoutesDropDown = (Spinner)findViewById(R.id.routesDropDown);
        mStopsDropDown = (Spinner)findViewById(R.id.stopsDropDown);

        mTxtEta = (TextView)findViewById(R.id.txtEta);

        getRoutes();
        mRoutesDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("nish","BOOBS");
                Log.d("nish", "Selected " + parent.getItemAtPosition(position).toString());
                int route_id = Integer.parseInt(parent.getItemAtPosition(position).toString());
                getRouteCoords(route_id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mStopsDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedStop = (Location) parent.getItemAtPosition(position);
                Log.d("nish", mSelectedStop.toString());
                if (mStopMarker == null) {
                    mStopMarker = mMap.addMarker(new MarkerOptions().position(mSelectedStop.coords));
                } else {
                    mStopMarker.setPosition(mSelectedStop.coords);
                }
                mStopMarker.setPosition(mSelectedStop.coords);
                getAndDisplayEta();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    void getAndDisplayEta () {
        LatLng origin = mBusMarker.getPosition();
        LatLng destination = mStopMarker.getPosition();
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                String.format("https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s&destinations=%s",
                        origin.latitude + "," + origin.longitude, destination.latitude + "," + destination.longitude),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("nish", "Got distance matric response: " + response);
                        try {
                            JSONObject jsonObj = new JSONObject(response);
                            String durationText = jsonObj.getJSONArray("rows")
                                    .getJSONObject(0)
                                    .getJSONArray("elements")
                                    .getJSONObject(0)
                                    .getJSONObject("duration")
                                    .getString("text");
                            Log.d("nish", "ETA: " + durationText);
                            mTxtEta.setText(durationText);
                        } catch (JSONException e) {
                            Log.d("nish", "Could not get distance matrix response: " + e.getMessage());
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
        Log.d("nish", "Making request: " + strReq.toString());
        queue.add(strReq);
    }


    void getRoutes () {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                "http://ec2-54-202-217-53.us-west-2.compute.amazonaws.com:8080/bus_routes",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("nish", "Got current coordinates" + response);
                        try {
                            JSONArray jsonArr = new JSONArray(response);
                            List<String> route_ids = new ArrayList<>();
                            for (int i = 0; i < jsonArr.length(); i++) {
                                JSONObject route = jsonArr.getJSONObject(i);
                                int route_id = route.getInt("route_id");
                                route_ids.add(String.valueOf(route_id));
                            }
                            mRoutesDropDown.setAdapter(new ArrayAdapter<String>(MapsActivity.this, R.layout.route_item, route_ids));
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
        Log.d("nish", "Making request: " + strReq.toString());
        queue.add(strReq);
    }

    private void getRouteCoords (int route_id) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest strReq = new StringRequest(
                Request.Method.GET,
                String.format("http://ec2-54-202-217-53.us-west-2.compute.amazonaws.com:8080/route_coords?route_id=%s", route_id),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("nish", "Got current coordinates" + response);
                        try {
                            JSONArray jsonArr = new JSONArray(response);
                            for (int i = 0; i < jsonArr.length(); i++) {
                                JSONObject route = jsonArr.getJSONObject(i);
                                int route_id = route.getInt("route_id");
                                double latitude = Double.parseDouble(route.getString("latitude"));
                                double longitude = Double.parseDouble(route.getString("longitude"));
                                Location loc = new Location(new LatLng(latitude, longitude), Time.valueOf("12:12:12"));
                                mRouteCoords.add(loc);
                                boolean isStop = route.getBoolean("is_stop");
                                if (isStop) mStops.add(loc);
                            }
                            Log.d("nish", mRouteCoords.toString());
                            mStopsDropDown.setAdapter(new ArrayAdapter<>(MapsActivity.this, R.layout.route_item, mStops));
                            mMapFragment.getMapAsync(MapsActivity.this);
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
        Log.d("nish", "Making request: " + strReq.toString());
        queue.add(strReq);
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
                            mBusMarker.setPosition(new LatLng(latitude, longitude));
                            Log.d("nish", "Current pos: " + mBusMarker.getPosition());
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
        Log.d("nish", "Making request: " + strReq.toString());
        queue.add(strReq);
    }


    void drawPath () {
        final List<LatLng> coords = new ArrayList<>();
        Collections.reverse(mRouteCoords);
        for (Location loc : removeAnomalousCoordinates(mRouteCoords)) coords.add(loc.coords);
        PolylineOptions path = new PolylineOptions().addAll(coords);
        mMap.addPolyline(path);
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
        drawPath();
        mBusMarker = mMap.addMarker(new MarkerOptions().position(mRouteCoords.get(currMarkerCoordIndex).coords));
        mBusMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus_small));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mMockCoords.get(0).coords, 12));
        getCurrentCoords((String) mRoutesDropDown.getSelectedItem());

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getCurrentCoords((String) mRoutesDropDown.getSelectedItem());
                getAndDisplayEta();
                Log.d("nish", "Called");
                handler.postDelayed(this, 5000);
            }
        }, 5000);
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
