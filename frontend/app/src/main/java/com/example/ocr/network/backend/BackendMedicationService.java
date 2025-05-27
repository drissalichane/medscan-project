package com.example.ocr.network.backend;

import com.example.ocr.model.BackendMedicationResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface BackendMedicationService {
    @GET("medications")
    Call<List<BackendMedicationResult>> getAllMedications();

    @GET("medications/{id}")
    Call<BackendMedicationResult> getMedicationById(@Path("id") Long id);
}
