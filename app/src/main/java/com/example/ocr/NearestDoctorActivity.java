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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.net.URLEncoder;
import java.text.Normalizer;
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
    private String currentCity = "Marrakech"; // Default city
    
    private final Map<String, String> doctorTypes = new HashMap<String, String>() {{
        put("General Practitioner", "medecin-generaliste");
        put("Dentist", "dentiste");
        put("Cardiologist", "cardiologue");
        put("Pediatrician", "pediatre");
        put("Dermatologist", "dermatologue");
        put("Ophthalmologist", "ophtalmologue");
        put("Orthopedist", "orthopediste");
        put("Gynecologist", "gynecologue");
        put("Neurologist", "neurologue");
        put("Psychiatrist", "psychiatre");
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
                
                // Search for doctors using doctori.ma
                searchDoctorsOnDoctori(doctorType);
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

    private void searchDoctorsOnDoctori(String doctorType) {
        executorService.execute(() -> {
            try {
                // Construct the URL for doctori.ma
                String url = String.format("https://www.doctori.ma/fr/medecin/%s/%s", 
                    doctorType.toLowerCase(), currentCity.toLowerCase());
                
                Log.d(TAG, "Searching doctors at URL: " + url);
                
                // Fetch the webpage
                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
                
                // Find all doctor address elements
                Elements addressElements = doc.select("span.adresse_doc");
                
                // Process first 5 doctors
                int count = 0;
                for (Element addressElement : addressElements) {
                    if (count >= 1) break;
                    
                    String address = addressElement.text().trim();
                    Log.d(TAG, "Found doctor address: " + address);
                    
                    // Geocode the address to get coordinates
                    geocodeAddress(address, doctorType);
                    count++;
                }
                
                if (count == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No doctors found in " + currentCity, 
                            Toast.LENGTH_SHORT).show();
                    });
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Error searching doctors on doctori.ma: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error searching for doctors: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private String simplifyAddress(String rawAddress) {
        // Remove accents and extra punctuation
        String clean = Normalizer.normalize(rawAddress, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "") // remove accents
                .replaceAll("(?i)\\b(bloc|resid|et|n°|apt|apartment|immeuble)\\b", "") // remove common extras
                .replaceAll("[^a-zA-Z0-9 ,]", "") // remove punctuation
                .replaceAll("\\s{2,}", " ") // collapse spaces
                .trim();
        return clean;
    }


    private void geocodeAddress(String address, String doctorType) {
        try {
            // Simplify the address (you may adjust your normalization as needed)
            String simplified = simplifyAddress(address);
            String query;
            // If the simplified address already contains the current city, don’t append it again.
            if (simplified.toLowerCase().contains(currentCity.toLowerCase())) {
                query = simplified + ", Morocco";
            } else {
                query = simplified + ", " + currentCity + ", Morocco";
            }
            String encodedAddress = java.net.URLEncoder.encode(query, "UTF-8");

            Log.d(TAG, "Geocoding query: " + query);
            Log.d(TAG, "Encoded URL parameter: " + encodedAddress);

            // Build the full URL for Nominatim search
            URL url = new URL("https://nominatim.openstreetmap.org/search?format=json&q=" + encodedAddress);
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

            JSONArray results = new JSONArray(response.toString());
            if (results.length() > 0) {
                JSONObject location = results.getJSONObject(0);
                double lat = Double.parseDouble(location.getString("lat"));
                double lon = Double.parseDouble(location.getString("lon"));

                Log.d(TAG, "Placing marker: " + doctorType + " - " + address + " at (" + lat + ", " + lon + ")");
                runOnUiThread(() -> {
                    Marker doctorMarker = new Marker(map);
                    doctorMarker.setPosition(new GeoPoint(lat, lon));
                    doctorMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    doctorMarker.setTitle(doctorType + " - " + address);
                    map.getOverlays().add(doctorMarker);
                    map.invalidate();
                });
            } else {
                Log.w(TAG, "Geocoding failed for address simplified : " + simplified + " and query : " + query);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error geocoding address: " + address + " - " + e.getMessage());
        }
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