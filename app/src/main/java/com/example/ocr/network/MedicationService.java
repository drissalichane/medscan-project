package com.example.ocr.network;

import com.example.ocr.model.OpenFdaResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MedicationService {
    @GET("label.json")
    Call<OpenFdaResponse> getMedicationInfo(@Query("search") String searchQuery);
}
