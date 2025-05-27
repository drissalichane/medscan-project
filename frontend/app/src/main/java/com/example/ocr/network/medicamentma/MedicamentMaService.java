package com.example.ocr.network.medicamentma;

import com.example.ocr.model.MedicamentMaResult;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MedicamentMaService {
    @GET("medicamentma")
    Call<String> getMedicationInfo(@Query("name") String name);
}
