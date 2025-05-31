package com.example.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.request.target.ViewTarget;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
    private String currentCity = "";
    private boolean isInfoWindowAdapterSet = false;
    
    private final Map<String, String> doctorTypes = new HashMap<String, String>() {{
        put("General Practitioner", "medecin-generaliste");
        put("Dentist", "chirurgien-dentiste");
        put("Cardiologist", "cardiologue");
        put("Pediatrician", "pediatre");
        put("Dermatologist", "dermatologue");
        put("Ophthalmologist", "ophtalmologue");
        put("Orthopedist", "chirurgien-orthopediste-et-traumatologue");
        put("Gynecologist", "gynecologue-obstetricien");
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
        toolbar.setBackgroundResource(R.color.primary_dark);
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
        
        // Set initial camera position to Casablanca
        LatLng casablanca = new LatLng(33.5899, -7.6033);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(casablanca, 12));
        
        // Initialize with nutritionists in Casablanca
        currentCity = "Casablanca";
        String nutritionistType = "nutritionniste";
        searchDoctorsOnDoctori(nutritionistType);
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
                
                // Get city from location
                Geocoder geocoder = new Geocoder(this);
                try {
                    List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1
                    );
                    if (addresses != null && !addresses.isEmpty()) {
                        currentCity = addresses.get(0).getLocality();
                        Log.d(TAG, "Current city: " + currentCity);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error getting city from location", e);
                    currentCity = "Unknown";
                }
                
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
                // Get all pages of doctors
                String baseUrl = String.format("https://www.doctori.ma/fr/medecin/%s/%s",
                        doctorType.toLowerCase(), currentCity.toLowerCase());
                Log.d(TAG, "Searching doctors at URL: " + baseUrl);

                // First get total number of pages
                Document firstPage = Jsoup.connect(baseUrl)
                        .userAgent("Mozilla/5.0")
                        .timeout(10000)
                        .get();
                
                // Get total number of pages
                Elements pagination = firstPage.select("div.pagination > a:last-child");
                int totalPages = 1;
                if (!pagination.isEmpty()) {
                    totalPages = Integer.parseInt(pagination.first().text());
                }
                Log.d(TAG, "Total pages: " + totalPages);

                // Process each page
                int doctorCount = 0;
                for (int page = 1; page <= totalPages && doctorCount < 5; page++) {
                    String pageUrl = baseUrl + (page > 1 ? "?page=" + page : "");
                    Document doc = Jsoup.connect(pageUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(10000)
                            .get();
                    
                    Elements doctorElements = doc.select("div.profil_left");
                    for (Element doctorElement : doctorElements) {
                        if (doctorCount >= 5) break;
                        
                        // Get doctor name
                        Element nameElement = doctorElement.select("span.dr-name-value").first();
                        String doctorName = "Dr. " + nameElement.text().trim();
                        
                        // Get address
                        Element addressElement = doctorElement.select("span.adresse_doc").first();
                        String address = addressElement.text().trim();
                        
                        // Get profile link and image URL
                        Element linkElement = doctorElement.select("a.btn_rdv_min").first();
                        String profileLink = linkElement.attr("href");
                        
                        // Get doctor image URL
                        Element imgElement = doctorElement.select("a.profil_img").first();
                        String imageUrl = imgElement.attr("style");
                        if (imageUrl.contains("background-image")) {
                            int start = imageUrl.indexOf("url(") + 4;
                            int end = imageUrl.indexOf(")", start);
                            imageUrl = imageUrl.substring(start, end).trim();
                        } else {
                            imageUrl = "https://cdn.doctori.ma/images/m_doctor_default_photo.svg";
                        }
                        
                        if (!address.isEmpty()) {
                            Log.d(TAG, "Found doctor: " + doctorName + " at " + address);
                            geocodeAddress(address, doctorType, doctorName, profileLink, imageUrl);
                            doctorCount++;
                        }
                    }
                }
                
                // Show success message
                runOnUiThread(() -> {
                    Toast.makeText(this, "Searching for doctors in " + currentCity,
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error searching doctors on doctori.ma: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error searching doctors: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void geocodeAddress(String address, String doctorType, String doctorName, String profileLink, String imageUrl) {
        try {
            Geocoder geocoder = new Geocoder(this);
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                Log.d(TAG, "Geocoded address: " + latLng);
                
                runOnUiThread(() -> {
                    if (mMap != null) {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(latLng)
                                .title(doctorName)
                                .snippet(address + "\nSpecialty: " + doctorType)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        
                        // Add marker click listener
                        Marker marker = mMap.addMarker(markerOptions);
                        marker.setTag(profileLink);
                        
                        // Store all data in marker tag
                        Bundle markerData = new Bundle();
                        markerData.putString("imageUrl", imageUrl);
                        markerData.putString("address", address);
                        markerData.putString("specialty", doctorType);
                        markerData.putString("profileLink", profileLink);
                        marker.setTag(markerData);
                        
                        // Set custom info window adapter only once
                        if (!isInfoWindowAdapterSet) {
                            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                                @Override
                                public View getInfoWindow(Marker marker) {
                                    return null; // Use getInfoContents
                                }

                                @Override
                                public View getInfoContents(Marker marker) {
                                    View view = getLayoutInflater().inflate(R.layout.custom_info_window, null);
                                    
                                    TextView title = view.findViewById(R.id.title);
                                    TextView snippet = view.findViewById(R.id.snippet);
                                    ImageView imageView = view.findViewById(R.id.doctorImage);
                                    
                                    Bundle markerData = (Bundle) marker.getTag();
                                    if (markerData != null) {
                                        String imageUrl = markerData.getString("imageUrl");
                                        String address = markerData.getString("address");

                                        if (address != null && address.length() > 1) {
                                            int middle = address.length() / 2;

                                            // Try to split on a space near the middle for cleaner break
                                            int splitIndex = address.lastIndexOf(" ", middle);
                                            if (splitIndex == -1) splitIndex = middle; // fallback if no space

                                            address = address.substring(0, splitIndex) + "\n" + address.substring(splitIndex + 1);
                                        }

                                        String specialty = markerData.getString("specialty");
                                        
                                        title.setText(marker.getTitle());
                                        snippet.setText("Address: " + address + "\nSpecialty: " + specialty);
                                        
                                        // Load image using Glide
                                        ViewTarget<ImageView, Drawable> into = Glide.with(NearestDoctorActivity.this)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.doctor)
                                            .error(R.drawable.doctor)
                                            .into(imageView);
                                    }
                                    
                                    return view;
                                }
                            });
                            
                            // Set info window click listener only once
                            mMap.setOnInfoWindowClickListener(clickedMarker -> {
                                Bundle clickedMarkerData = (Bundle) clickedMarker.getTag();
                                if (clickedMarkerData != null) {
                                    String clickedProfileLink = clickedMarkerData.getString("profileLink");
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedProfileLink));
                                    startActivity(browserIntent);
                                }
                            });
                            
                            isInfoWindowAdapterSet = true;
                        }
                    }
                });
            } else {
                Log.e(TAG, "No location found for address: " + address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error geocoding address: " + e.getMessage());
        }
    }

    // Remove this method since we're now getting the doctor name directly from the page
    // (This is a comment, not a method)

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
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