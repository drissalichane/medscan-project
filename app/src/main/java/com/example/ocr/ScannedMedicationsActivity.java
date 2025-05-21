package com.example.ocr;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.adapter.MedicationAdapter;
import com.example.ocr.model.BackendMedicationResult;

import java.util.ArrayList;
import java.util.List;

public class ScannedMedicationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DatabaseHelper databaseHelper;
    private MedicationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_medications); // Reusing the same layout

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Scanned Medications");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewMedications);
        emptyView = findViewById(R.id.textViewEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapter
        adapter = new MedicationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Load scanned medications
        loadScannedMedications();
    }

    private void loadScannedMedications() {
        Cursor cursor = databaseHelper.getMedicationInfo("");
        List<BackendMedicationResult> medications = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                BackendMedicationResult medication = new BackendMedicationResult();
                medication.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                medication.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
                medication.setSideEffects(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SIDE_EFFECTS)));
                medications.add(medication);
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (medications.isEmpty()) {
            showEmptyView();
        } else {
            showMedications(medications);
        }
    }

    private void showMedications(List<BackendMedicationResult> medications) {
        recyclerView.setVisibility(android.view.View.VISIBLE);
        emptyView.setVisibility(android.view.View.GONE);
        adapter.updateMedications(medications);
    }

    private void showEmptyView() {
        recyclerView.setVisibility(android.view.View.GONE);
        emptyView.setVisibility(android.view.View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 