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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.location.Geocoder;
import android.location.Address;
import java.io.IOException;
import java.util.List;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NearestDoctorActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    static final String TAG = "GoogleMap";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ChipGroup doctorTypesChipGroup;
    private ExecutorService executorService;
    private String currentCity = "Marrakech";
    
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

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Find Nearest Doctors");

        // Initialize executor service for network calls
        executorService = Executors.newSingleThreadExecutor();

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up doctor type chips
        doctorTypesChipGroup = findViewById(R.id.doctorTypesChipGroup);
        setupDoctorTypeChips();

        // Set up Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        } else {
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
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Current location: " + currentLatLng.latitude + ", " + currentLatLng.longitude);
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
            } else {
                Log.e(TAG, "Could not get current location");
                Toast.makeText(this, "Could not get your location. Please check your GPS settings.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchNearbyDoctors(String doctorType) {
        if (mMap == null) {
            Log.e(TAG, "Map is not initialized");
            return;
        }
        Log.d(TAG, "Searching for doctor type: " + doctorType);
        mMap.clear();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted for nearby search");
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                searchDoctorsOnDoctori(doctorType);
            } else {
                Log.e(TAG, "Could not get location for nearby search");
                Toast.makeText(this, "Could not get your location. Please check your GPS settings.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchDoctorsOnDoctori(String doctorType) {
        executorService.execute(() -> {
            try {
                String url = String.format("https://www.doctori.ma/fr/medecin/%s/%s",
                        doctorType.toLowerCase(), currentCity.toLowerCase());
                Log.d(TAG, "Searching doctors at URL: " + url);
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();
                Elements addressElements = doc.select("span.adresse_doc");
                int count = 0;
                for (Element addressElement : addressElements) {
                    if (count >= 5) break;
                    String address = addressElement.text().trim();
                    Log.d(TAG, "Found doctor address: " + address);
                    geocodeAddressWithAI(address, doctorType);
                    count++;
                }
                if (count == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "No doctors found in " + currentCity,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching doctors on doctori.ma: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error searching for doctors: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void geocodeAddressWithAI(String rawAddress, String doctorType) {
        executorService.execute(() -> {
            try {
                // Clean up the address by trimming and normalizing
                String cleanedAddress = rawAddress.trim();

                // Use Android's Geocoder class for geocoding
                Geocoder geocoder = new Geocoder(this);
                List<Address> addresses = geocoder.getFromLocationName(cleanedAddress, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    double lat = address.getLatitude();
                    double lng = address.getLongitude();
                    
                    Log.d(TAG, "Placing marker: " + doctorType + " - " + cleanedAddress + " at (" + lat + ", " + lng + ")");
                    runOnUiThread(() -> {
                        if (mMap != null) {
                            LatLng position = new LatLng(lat, lng);
                            mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(doctorType + " - " + cleanedAddress));
                        }
                    });
                } else {
                    Log.w(TAG, "Geocoding failed for address: " + cleanedAddress);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error geocoding address: " + rawAddress + " - " + e.getMessage());
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
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
} 