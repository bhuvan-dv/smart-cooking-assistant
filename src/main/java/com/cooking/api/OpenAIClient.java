package com.cooking.api;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OpenAIClient {
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final OkHttpClient httpClient;

    public OpenAIClient(String apiKey, String model, double temperature) {
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;

        // Configure HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String generateRecipe(String prompt) throws IOException {
        // Create the message object
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);

        // Create messages array - FIXED: Use JSONArray instead of JSONObject[]
        JSONArray messages = new JSONArray();
        messages.put(message);

        // Create request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);

        // Create the HTTP request
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                throw new IOException("OpenAI API request failed with code " + response.code() + ": " + errorBody);
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Extract the generated content
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            throw new IOException("Error calling OpenAI API: " + e.getMessage(), e);
        }
    }
}