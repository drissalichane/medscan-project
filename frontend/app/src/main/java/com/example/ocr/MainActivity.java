package com.example.ocr;

import android.Manifest;
import android.content.ContentValues;
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
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.ocr.network.ApiClient;
import com.example.ocr.network.MedicationService;
import com.example.ocr.model.OpenFdaResponse;

import org.json.JSONArray;

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
    private Button languageToggleButton;
    private boolean isFrench = false;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private BackendMedicationService backendMedicationService;
    private MedicamentMaService medicamentMaService;

    private StringBuilder medicationInfoBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start TimeTrackerService
        Intent timeTrackerIntent = new Intent(this, TimeTrackerService.class);
        startService(timeTrackerIntent);
        setContentView(R.layout.activity_main);

        // Initialize language toggle button
        languageToggleButton = findViewById(R.id.languageToggleButton);
        languageToggleButton.setText("EN"); // Default to English
        languageToggleButton.setEnabled(false); // Disable the button initially

                languageToggleButton.setOnClickListener(v -> {
            isFrench = !isFrench; // Toggle language
            languageToggleButton.setText(isFrench ? "FR" : "EN");
        
            if (isFrench) {
                // Display the translated text directly without reformatting
                translateText(medicationInfoBuilder.toString(), translatedText -> {
                    runOnUiThread(() -> {
                        textMedicationInfo.setText(translatedText); // Display translated text directly
                    });
                });
            } else {
                // Display the original English text with formatting
                String formattedText = formatMedInfoText(medicationInfoBuilder.toString());
                textMedicationInfo.setText(formattedText);
            }
        });

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundResource(R.color.primary_dark);
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
        } else if (id == R.id.nav_reminders) {
            startActivity(new Intent(this, MedicationRemindersActivity.class));
            drawerLayout.closeDrawers();
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
                    if (scannedText.isEmpty()) {
                        textResult.setText("No text detected.");
                        return;
                    }

                    textResult.setText(scannedText); // Display scanned text directly

                    // Extract medication name from scanned text
                    GeminiHelper geminiHelper = new GeminiHelper();
                    geminiHelper.extractMedicationName(scannedText, new GeminiHelper.GeminiCallback() {
                        @Override
                        public void onSuccess(String medicationName) {
                            runOnUiThread(() -> {
                                textResult.append("\n\nDetected medication: " + medicationName);
                                fetchMedicationInfo(medicationName); // Fetch medication info
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

        private void fetchMedicationInfo(String medicationName) {
        Log.d("MedInfo", "Starting fetchMedicationInfo for: " + medicationName);
    
        // Fetch medication info from OpenFDA
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
    
                    // Build the medication info string
                    StringBuilder rawMedicationInfo = new StringBuilder();
                    if (result.purpose != null && !result.purpose.isEmpty()) {
                        rawMedicationInfo.append("\nPurpose:\n").append(String.join("; ", result.purpose)).append("\n");
                    }
                    if (result.indications_and_usage != null && !result.indications_and_usage.isEmpty()) {
                        rawMedicationInfo.append("\nUsage:\n").append(String.join("; ", result.indications_and_usage)).append("\n");
                    }
                    if (result.warnings != null && !result.warnings.isEmpty()) {
                        rawMedicationInfo.append("\nWarnings:\n").append(String.join("; ", result.warnings)).append("\n");
                    }
    
                    // Format and display the English text
                    String formattedText = formatMedInfoText(rawMedicationInfo.toString());
                    medicationInfoBuilder.append(formattedText);
                    runOnUiThread(() -> {
                        textMedicationInfo.setText(formattedText);
                        languageToggleButton.setEnabled(true); // Enable the language toggle button
                    });
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

      private void saveToDatabase(String medicationName, String formattedText) {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
    
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, medicationName); // Use the correct column name for medication name
        values.put(DatabaseHelper.COLUMN_PURPOSE, formattedText); // Save formatted text in the appropriate column (e.g., purpose)
    
        long rowId = db.insert(DatabaseHelper.TABLE_NAME, null, values);
        if (rowId != -1) {
            Log.d("Database", "Medication info saved successfully: " + medicationName);
        } else {
            Log.e("Database", "Failed to save medication info: " + medicationName);
        }
    
        db.close();
    }

    private String formatMedInfoText(String text) {
        // 1. Place each bullet on a new line (with a blank line before if not at start)
        text = text.replaceAll("\\s*•", "\n•");

        // 2. Add a newline after every colon (:)
        text = text.replaceAll(":", ":\n");

        // 3. After a period, insert a newline and "- " only if there's text after it
        text = text.replaceAll("\\.(\\s*)(?=\\S)", ".\n\n- ");

        // 4. Remove any lines that are just a dash (e.g., "\n-")
        text = text.replaceAll("\n-\\s*(\n|$)", "\n");

        // 5. Clean up: remove leading dash if it appears at the very start
        text = text.replaceAll("^\n?-\\s*", "");

        return text.trim();
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

               private void translateText(String textToTranslate, TranslationCallback callback) {
                String apiEndpoint = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=fr&dt=t&q=" + Uri.encode(textToTranslate);
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            
                Log.d("Translation", "API Endpoint: " + apiEndpoint); // Log the API endpoint
            
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(apiEndpoint)
                        .get()
                        .build();
            
                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        Log.e("Translation", "Translation failed: " + e.getMessage(), e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Translation failed", Toast.LENGTH_SHORT).show());
                    }
            
                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                        Log.d("Translation", "Response Code: " + response.code()); // Log the response code
                        Log.d("Translation", "Response Message: " + response.message()); // Log the response message
            
                        if (!response.isSuccessful()) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Translation failed: Unsuccessful response", Toast.LENGTH_SHORT).show());
                            return;
                        }
            
                        String responseString = response.body().string();
                        Log.d("Translation", "Response Body: " + responseString); // Log the response body
            
                        try {
                            // Parse the response using JSON
                            JSONArray responseArray = new JSONArray(responseString);
                            JSONArray translationsArray = responseArray.getJSONArray(0); // Main translations array
            
                            StringBuilder translatedTextBuilder = new StringBuilder();
                            for (int i = 0; i < translationsArray.length(); i++) {
                                JSONArray translationItem = translationsArray.getJSONArray(i);
                                String translation = translationItem.getString(0); // Extract the actual translation
                                translatedTextBuilder.append(translation).append("\n\n");
                            }
            
                            String translatedText = translatedTextBuilder.toString().trim();
                            Log.d("Translation", "Extracted Translation: " + translatedText); // Log the extracted translation
                            runOnUiThread(() -> callback.onTranslationComplete(translatedText));
                        } catch (Exception e) {
                            Log.e("Translation", "Error parsing translation response", e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Translation failed: Could not parse response", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }
    // Callback interface for translation
    interface TranslationCallback {
        void onTranslationComplete(String translatedText);
    }
}
