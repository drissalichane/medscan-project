package com.example.ocr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NearestDoctorActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final String TAG = "OpenStreetMap";
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private ChipGroup doctorTypesChipGroup;
    private ExecutorService executorService;
    
    private final Map<String, String> doctorTypes = new HashMap<String, String>() {{
        put("General Practitioner", "doctor");
        put("Dentist", "dentist");
        put("Cardiologist", "cardiologist");
        put("Pediatrician", "pediatrician");
        put("Dermatologist", "dermatologist");
        put("Ophthalmologist", "eye doctor");
        put("Orthopedist", "orthopedic");
        put("Gynecologist", "gynecologist");
        put("Neurologist", "neurologist");
        put("Psychiatrist", "psychiatrist");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearest_doctor);

        // Initialize OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidTileCache(getExternalFilesDir(null));

        // Initialize executor service for network calls
        executorService = Executors.newSingleThreadExecutor();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Find Nearest Doctors");

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up map
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(18.0);
        map.setMinZoomLevel(4.0);
        map.setMaxZoomLevel(19.0);

        // Set up doctor type chips
        doctorTypesChipGroup = findViewById(R.id.doctorTypesChipGroup);
        setupDoctorTypeChips();

        // Request location permission and enable location
        enableMyLocation();
    }

    private void setupDoctorTypeChips() {
        for (String doctorType : doctorTypes.keySet()) {
            Chip chip = new Chip(this);
            chip.setText(doctorType);
            chip.setCheckable(true);
            chip.setClickable(true);
            doctorTypesChipGroup.addView(chip);
        }

        doctorTypesChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                Chip chip = group.findViewById(checkedId);
                String selectedType = doctorTypes.get(chip.getText().toString());
                searchNearbyDoctors(selectedType);
            }
        });
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.getController().setZoom(15.0);
            Log.d(TAG, "Location enabled on map");
            getCurrentLocation();
        } else {
            Log.d(TAG, "Requesting location permission");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }
        
        Log.d(TAG, "Getting current location");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Current location: " + currentPoint.getLatitude() + ", " + currentPoint.getLongitude());
                
                // Clear existing overlays
                map.getOverlays().clear();
                
                // Add marker for current location
                Marker currentLocationMarker = new Marker(map);
                currentLocationMarker.setPosition(currentPoint);
                currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                currentLocationMarker.setTitle("Your Location");
                map.getOverlays().add(currentLocationMarker);
                
                // Center map on current location
                map.getController().animateTo(currentPoint);
                map.getController().setZoom(18.0); // Zoom to street level
            } else {
                Log.e(TAG, "Could not get current location");
                Toast.makeText(this, "Could not get your location. Please check your GPS settings.", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchNearbyDoctors(String doctorType) {
        if (map == null) {
            Log.e(TAG, "Map is not initialized");
            return;
        }

        Log.d(TAG, "Searching for doctor type: " + doctorType);
        map.getOverlays().clear();
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted for nearby search");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Log.d(TAG, "Starting nearby search at location: " + 
                    location.getLatitude() + ", " + location.getLongitude());
                
                // Add current location marker
                addCurrentLocationMarker(location);
                
                // Search for doctors using Overpass API
                searchDoctorsWithOverpass(location, doctorType);
            } else {
                Log.e(TAG, "Could not get location for nearby search");
                Toast.makeText(this, "Could not get your location. Please check your GPS settings.", 
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addCurrentLocationMarker(Location location) {
        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        Marker currentLocationMarker = new Marker(map);
        currentLocationMarker.setPosition(currentPoint);
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        currentLocationMarker.setTitle("Your Location");
        map.getOverlays().add(currentLocationMarker);
        map.getController().animateTo(currentPoint);
    }

    private void searchDoctorsWithOverpass(Location location, String doctorType) {
        executorService.execute(() -> {
            try {
                // Build Overpass API query
                String query = String.format(
                    "[out:json][timeout:25];" +
                    "(" +
                    "  node[\"amenity\"=\"clinic\"](around:5000,%f,%f);" +
                    "  node[\"amenity\"=\"doctors\"](around:5000,%f,%f);" +
                    "  node[\"healthcare\"=\"doctor\"](around:5000,%f,%f);" +
                    ");" +
                    "out body;>;out skel qt;",
                    location.getLatitude(), location.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    location.getLatitude(), location.getLongitude()
                );

                // Encode query for URL
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://overpass-api.de/api/interpreter?data=" + encodedQuery);

                // Make HTTP request
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "MedScan/1.0");

                // Read response
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray elements = jsonResponse.getJSONArray("elements");

                // Process results on UI thread
                runOnUiThread(() -> {
                    if (elements.length() > 0) {
                        for (int i = 0; i < elements.length(); i++) {
                            try {
                                JSONObject element = elements.getJSONObject(i);
                                if (element.has("lat") && element.has("lon")) {
                                    double lat = element.getDouble("lat");
                                    double lon = element.getDouble("lon");
                                    
                                    // Create marker for doctor location
                                    Marker doctorMarker = new Marker(map);
                                    doctorMarker.setPosition(new GeoPoint(lat, lon));
                                    doctorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    
                                    // Set title from tags if available
                                    if (element.has("tags")) {
                                        JSONObject tags = element.getJSONObject("tags");
                                        if (tags.has("name")) {
                                            doctorMarker.setTitle(tags.getString("name"));
                                        } else {
                                            doctorMarker.setTitle(doctorType + " Clinic");
                                        }
                                    } else {
                                        doctorMarker.setTitle(doctorType + " Clinic");
                                    }
                                    
                                    map.getOverlays().add(doctorMarker);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing doctor location: " + e.getMessage());
                            }
                        }
                        map.invalidate(); // Refresh map
                        Toast.makeText(this, "Found " + elements.length() + " medical facilities nearby", 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No medical facilities found nearby", 
                            Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error searching for doctors: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error searching for doctors: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
                enableMyLocation();
            } else {
                Log.e(TAG, "Location permission denied");
                Toast.makeText(this, "Location permission is required to find nearby doctors",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 