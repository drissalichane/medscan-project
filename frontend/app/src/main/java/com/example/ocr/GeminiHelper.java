package com.example.ocr;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;


public class GeminiHelper {
    private static final String GEMINI_FLASH_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyAKxsguQPheD6QMFMl-T_I0YLnhJ1Af2TM";
    private static final String GEMINI_PRO_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyAKxsguQPheD6QMFMl-T_I0YLnhJ1Af2TM";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    public interface GeminiCallback {
        void onSuccess(String medicationName);
        void onFailure(Exception e);
    }

    public void extractMedicationName(String ocrText, GeminiCallback callback) {
        String prompt = "Given this OCR result from a medication box:\n\"" + ocrText.replace("\"", "\\\"") + "\"\n First filter any unnecessary text that is not the actual"+
        "medication name nly return Aspirin and What is the main medication name in English? Only return the English medication name.";
        Log.d("GEMINI AI", "Prompt sent to Gemini: " + prompt);
        String json = "{\n" +
                "  \"contents\": [{\"parts\": [{\"text\": \"" + prompt.replace("\"", "\\\"") + "\"}]}]\n" +
                "}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(GEMINI_FLASH_API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("GEMINI AI", "Raw response from Gemini: " + responseBody);
                if (!response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                    return;
                }
                String medName = parseGeminiResponse(responseBody);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(medName));
            }
        });
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text").trim();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void getMedicalAdvice(String symptoms, GeminiCallback callback) {
        String concisePrompt = symptoms + "\n\nProvide a brief response (2-3 sentences each):\n" +
                "1. Most likely cause based on symptoms and context\n" +
                "2. Quick remedy suggestion (considering age and allergies)\n" +
                "3. When to see doctor (urgent if severe symptoms)";
        
        Log.d("DOCTOR AI", "Prompt sent to Gemini: " + concisePrompt);
        
        String json = "{\n" +
                "  \"contents\": [{\"parts\": [{\"text\": \"" + concisePrompt.replace("\"", "\\\"") + "\"}]}]\n" +
                "}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(GEMINI_FLASH_API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("DOCTOR AI", "Raw response from Gemini: " + responseBody);
                if (!response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onFailure(new IOException("Unexpected code " + response))
                    );
                    return;
                }
                String advice = parseGeminiResponse(responseBody);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(advice));
            }
        });
    }
} 