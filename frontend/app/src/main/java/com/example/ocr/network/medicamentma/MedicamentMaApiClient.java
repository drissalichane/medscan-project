package com.example.ocr.network.medicamentma;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MedicamentMaApiClient {
    private static final String BASE_URL = "http://192.168.200.26:8080/api/"; // Backend base URL

    private static Retrofit retrofit = null;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
