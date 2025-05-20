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

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.HashMap;
import java.util.Map;

public class NearestDoctorActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final String TAG = "OpenStreetMap";
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private ChipGroup doctorTypesChipGroup;
    
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

        // Set up doctor type chips
        doctorTypesChipGroup = findViewById(R.id.doctorTypesChipGroup);
        setupDoctorTypeChips();
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
                map.getController().animateTo(currentPoint);
            } else {
                Log.e(TAG, "Could not get current location");
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
                // TODO: Implement nearby search using Overpass API or similar
                // For now, we'll just show a message
                Toast.makeText(this, "Searching for " + doctorType + " near you...", 
                    Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Could not get location for nearby search");
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
} 