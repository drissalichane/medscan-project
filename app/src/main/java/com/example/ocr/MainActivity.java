package com.example.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.exifinterface.media.ExifInterface;

import com.example.ocr.model.BackendMedicationResult;
import com.example.ocr.model.MedicationResult;
import com.example.ocr.network.backend.BackendApiClient;
import com.example.ocr.network.backend.BackendMedicationService;
import com.example.ocr.network.medicamentma.MedicamentMaApiClient;
import com.example.ocr.network.medicamentma.MedicamentMaService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import com.example.ocr.network.ApiClient;
import com.example.ocr.network.MedicationService;
import com.example.ocr.model.OpenFdaResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private ImageView imagePreview;
    private TextView textResult;
    private TextView textMedicationInfo;
    private FloatingActionButton fabActions;
    private String currentPhotoPath;
    private MedicationService medicationService;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private BackendMedicationService backendMedicationService;
    private MedicamentMaService medicamentMaService;

    private StringBuilder medicationInfoBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        // Set up navigation drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navView);
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        medicationService = ApiClient.getRetrofitInstance().create(MedicationService.class);
        backendMedicationService = BackendApiClient.getRetrofitInstance().create(BackendMedicationService.class);
        medicamentMaService = MedicamentMaApiClient.getRetrofitInstance().create(MedicamentMaService.class);

        Button buttonCapture = findViewById(R.id.buttonCapture);
        imagePreview = findViewById(R.id.imagePreview);
        textResult = findViewById(R.id.textResult);
        textMedicationInfo = findViewById(R.id.textMedicationInfo);
        fabActions = findViewById(R.id.fabActions);

        fabActions.setOnClickListener(v -> {
            if (medicationInfoBuilder.length() == 0) {
                showMessageDialog("No medication information available.");
            } else {
                showMessageDialog(medicationInfoBuilder.toString());
            }
        });

        buttonCapture.setOnClickListener(v -> {
            medicationInfoBuilder.setLength(0);
            textMedicationInfo.setText("");
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_scan) {
            // Already on scan screen
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_medications) {
            startActivity(new Intent(this, MyMedicationsActivity.class));
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_scanned_medications) {
            startActivity(new Intent(this, ScannedMedicationsActivity.class));
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_chatbot) {
            startActivity(new Intent(this, ChatbotActivity.class));
            drawerLayout.closeDrawers();
        } else if (id == R.id.nav_nearest_doctor) {
            Intent intent = new Intent(this, NearestDoctorActivity.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile("IMG_" + timeStamp, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.ocr.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File imgFile = new File(currentPhotoPath);
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                        bitmap = getCorrectlyOrientedImage(bitmap, currentPhotoPath);
                        imagePreview.setImageBitmap(bitmap);
                        processImageWithOCR(bitmap);
                    }
                }
            }
    );

    private Bitmap getCorrectlyOrientedImage(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(new FileInputStream(imagePath));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private void processImageWithOCR(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String scannedText = visionText.getText();
                    textResult.setText(scannedText.isEmpty() ? "No text detected." : scannedText);

                    // Use GeminiHelper to extract and translate medication name
                    GeminiHelper geminiHelper = new GeminiHelper();
                    geminiHelper.extractMedicationName(scannedText, new GeminiHelper.GeminiCallback() {
                        @Override
                        public void onSuccess(String medicationName) {
                            runOnUiThread(() -> {
                                textResult.append("\n\nDetected medication: " + medicationName);
                                processScannedText(medicationName);
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            runOnUiThread(() -> {
                                textResult.append("\n\nFailed to extract medication name: " + e.getMessage());
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR", "Text recognition failed", e);
                    Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show();
                });
    }

    private List<String> extractValidWords(String scannedText) {
        List<String> validWords = new ArrayList<>();

        // Split text into words (based on spaces, punctuation)
        String[] words = scannedText.split("\\s+");

        for (String word : words) {
            // Remove words that:
            // 1. Are 2 letters or shorter
            // 2. Contain special characters like "+"
            if (word.length() > 2 && !word.matches(".*[+].*")) {
                validWords.add(word);
            }
        }
        return validWords;
    }

    private void processScannedText(String scannedText) {
        List<String> validWords = extractValidWords(scannedText);
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        for (String word : validWords) {
            // Save to database
            databaseHelper.insertMedication(db, word, "Scanned medication", "Side effects will be updated when verified");
            
            // Fetch additional info
            fetchMedicationInfo(word);
            fetchMedicationInfoFromBackend(word);
        }
    }

    private void fetchMedicationInfo(String medicationName) {
        String query = "spl_product_data_elements:" + medicationName + "*";

        Call<OpenFdaResponse> call = medicationService.getMedicationInfo(query);
        call.enqueue(new Callback<OpenFdaResponse>() {
            @Override
            public void onResponse(Call<OpenFdaResponse> call, Response<OpenFdaResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().results != null && !response.body().results.isEmpty()) {
                    MedicationResult result = response.body().results.get(0);
                    
                    // Clear previous content
                    textMedicationInfo.setText("");
                    medicationInfoBuilder.setLength(0);
                    
                    // Store all information in the builder for later use
                    if (result.purpose != null) {
                        medicationInfoBuilder.append("PURPOSE:").append(String.join(", ", result.purpose)).append("\n");
                    }
                    if (result.indications_and_usage != null) {
                        medicationInfoBuilder.append("INDICATIONS:").append(String.join(", ", result.indications_and_usage)).append("\n");
                    }
                    if (result.warnings != null) {
                        medicationInfoBuilder.append("WARNINGS:").append(String.join(", ", result.warnings)).append("\n");
                    }
                    if (result.precautions != null) {
                        medicationInfoBuilder.append("PRECAUTIONS:").append(String.join(", ", result.precautions)).append("\n");
                    }
                    if (result.general_precautions != null) {
                        medicationInfoBuilder.append("GENERAL_PRECAUTIONS:").append(String.join(", ", result.general_precautions)).append("\n");
                    }
                    if (result.adverse_reactions != null) {
                        medicationInfoBuilder.append("ADVERSE_REACTIONS:").append(String.join(", ", result.adverse_reactions)).append("\n");
                    }
                    if (result.overdosage != null) {
                        medicationInfoBuilder.append("OVERDOSAGE:").append(String.join(", ", result.overdosage)).append("\n");
                    }
                    if (result.do_not_use != null) {
                        medicationInfoBuilder.append("DO_NOT_USE:").append(String.join(", ", result.do_not_use)).append("\n");
                    }
                    if (result.stop_use != null) {
                        medicationInfoBuilder.append("STOP_USE:").append(String.join(", ", result.stop_use)).append("\n");
                    }
                    if (result.when_use != null) {
                        medicationInfoBuilder.append("WHEN_USE:").append(String.join(", ", result.when_use)).append("\n");
                    }
                    if (result.ask_doctor != null) {
                        medicationInfoBuilder.append("ASK_DOCTOR:").append(String.join(", ", result.ask_doctor)).append("\n");
                    }
                    if (result.ask_doctor_or_pharmacist != null) {
                        medicationInfoBuilder.append("ASK_DOCTOR_OR_PHARMACIST:").append(String.join(", ", result.ask_doctor_or_pharmacist)).append("\n");
                    }

                    // Display basic information in the TextView
                    StringBuilder basicInfo = new StringBuilder();
                    basicInfo.append("Medication: ").append(medicationName).append("\n\n");
                    
                    if (result.purpose != null && !result.purpose.isEmpty()) {
                        basicInfo.append("Purpose: ").append(result.purpose.get(0)).append("\n\n");
                    }
                    if (result.indications_and_usage != null && !result.indications_and_usage.isEmpty()) {
                        basicInfo.append("Usage: ").append(result.indications_and_usage.get(0)).append("\n\n");
                    }
                    if (result.warnings != null && !result.warnings.isEmpty()) {
                        basicInfo.append("Warning: ").append(result.warnings.get(0)).append("\n\n");
                    }
                    
                    textMedicationInfo.setText(basicInfo.toString());
                    
                    // Show a toast to indicate more information is available
                    Toast.makeText(MainActivity.this, "Click the info button for more details", Toast.LENGTH_LONG).show();
                } else {
                    textMedicationInfo.setText("No matches found for: " + medicationName);
                }
            }

            @Override
            public void onFailure(Call<OpenFdaResponse> call, Throwable t) {
                Log.e("OpenFDA", "API call failed", t);
                textMedicationInfo.setText("Failed to fetch medication information");
            }
        });
    }

    // New method to fetch medication info from backend
    private void fetchMedicationInfoFromBackend(String medicationName) {
        backendMedicationService.getAllMedications().enqueue(new Callback<List<BackendMedicationResult>>() {
            @Override
            public void onResponse(Call<List<BackendMedicationResult>> call, Response<List<BackendMedicationResult>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BackendMedicationResult> medications = response.body();
                    boolean found = false;
                    for (BackendMedicationResult med : medications) {
                        if (med.getName() != null && med.getName().equalsIgnoreCase(medicationName)) {
                            found = true;
                            medicationInfoBuilder.append("\nValid Medication: ").append(med.getName()).append(" (from Backend)");
                            if (med.getSideEffects() != null)
                                medicationInfoBuilder.append("\nSide Effects: ").append(med.getSideEffects());
                            if (med.getInteractions() != null)
                                medicationInfoBuilder.append("\nInteractions: ").append(med.getInteractions());
                            break;
                        }
                    }
                    if (!found) {
                        medicationInfoBuilder.append("\nNo matches found for: ").append(medicationName);
                    }
                } else {
                    medicationInfoBuilder.append("\nNo matches found for: ").append(medicationName);
                }
            }

            @Override
            public void onFailure(Call<List<BackendMedicationResult>> call, Throwable t) {
                Log.e("BackendAPI", "API call failed", t);
            }
        });
    }

    // New method to fetch medication info from medicament.ma backend
    private void fetchMedicationInfoFromMedicamentMa(String medicationName) {
        medicamentMaService.getMedicationInfo(medicationName).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String data = response.body();
                    medicationInfoBuilder.append("\nMedicament.ma data for ").append(medicationName).append(":\n").append(data);
                } else {
                    medicationInfoBuilder.append("\nNo medicament.ma data found for: ").append(medicationName);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("MedicamentMaAPI", "API call failed", t);
            }
        });
    }

    private void showMessageDialog(String message) {
        // Split the message into different sections
        String[] sections = message.split("\n");
        List<String> availableSections = new ArrayList<>();
        Map<String, String> sectionContent = new HashMap<>();

        for (String section : sections) {
            if (section.contains(":")) {
                String[] parts = section.split(":", 2);
                if (parts.length == 2) {
                    String title = parts[0].trim();
                    String content = parts[1].trim();
                    if (!content.isEmpty()) {
                        availableSections.add(title);
                        sectionContent.put(title, content);
                    }
                }
            }
        }

        if (availableSections.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Medication Information")
                    .setMessage("No detailed information available")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Create buttons for each section
        String[] sectionTitles = availableSections.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Information Category")
                .setItems(sectionTitles, (dialog, which) -> {
                    String selectedTitle = sectionTitles[which];
                    String content = sectionContent.get(selectedTitle);
                    
                    // Show the content in a new dialog
                    new AlertDialog.Builder(this)
                            .setTitle(selectedTitle)
                            .setMessage(content)
                            .setPositiveButton("OK", null)
                            .show();
                })
                .setPositiveButton("Close", null)
                .show();
    }
}
