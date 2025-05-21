package com.example.ocr;

import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.adapter.BackendMedicationAdapter;
import com.example.ocr.model.BackendMedicationResult;
import com.example.ocr.network.backend.BackendApiClient;
import com.example.ocr.network.backend.BackendMedicationService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyMedicationsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private BackendMedicationService medicationService;
    private BackendMedicationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_medications);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Medications");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewMedications);
        emptyView = findViewById(R.id.textViewEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize adapter
        adapter = new BackendMedicationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Set up search view
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        // Initialize service
        medicationService = BackendApiClient.getRetrofitInstance().create(BackendMedicationService.class);

        // Load medications
        loadMedications();
    }

    private void loadMedications() {
        medicationService.getAllMedications().enqueue(new Callback<List<BackendMedicationResult>>() {
            @Override
            public void onResponse(Call<List<BackendMedicationResult>> call, Response<List<BackendMedicationResult>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BackendMedicationResult> medications = response.body();
                    if (medications.isEmpty()) {
                        showEmptyView();
                    } else {
                        showMedications(medications);
                    }
                } else {
                    showError("Failed to load medications");
                }
            }

            @Override
            public void onFailure(Call<List<BackendMedicationResult>> call, Throwable t) {
                showError("Error: " + t.getMessage());
            }
        });
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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        showEmptyView();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 