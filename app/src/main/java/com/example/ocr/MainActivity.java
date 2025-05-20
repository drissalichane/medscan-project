package com.example.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.exifinterface.media.ExifInterface;

import com.example.ocr.model.MedicationResult;
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
    private String currentPhotoPath;
    private MedicationService medicationService;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

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

        Button buttonCapture = findViewById(R.id.buttonCapture);
        imagePreview = findViewById(R.id.imagePreview);
        textResult = findViewById(R.id.textResult);
        textMedicationInfo = findViewById(R.id.textMedicationInfo);

        buttonCapture.setOnClickListener(v -> {
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
            // TODO: Implement medications list screen
            Toast.makeText(this, "Medications list coming soon!", Toast.LENGTH_SHORT).show();
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
                                fetchMedicationInfo(medicationName);
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

        for (String word : validWords) {
            fetchMedicationInfo(word);
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
                    textMedicationInfo.append("\nValid Medication: " + medicationName);

                    if (result.indications_and_usage != null)
                        textMedicationInfo.append("\nIndications: " + result.indications_and_usage);
                    if (result.warnings != null)
                        textMedicationInfo.append("\nWarnings: " + result.warnings);
                    if (result.adverse_reactions != null)
                        textMedicationInfo.append("\nAdverse Reactions: " + result.adverse_reactions);
                } else {
                    textMedicationInfo.append("\nNo matches found for: " + medicationName);
                }
            }

            @Override
            public void onFailure(Call<OpenFdaResponse> call, Throwable t) {
                Log.e("OpenFDA", "API call failed", t);
            }
        });
    }
}
