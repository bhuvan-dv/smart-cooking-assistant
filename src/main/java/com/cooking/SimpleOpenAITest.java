package com.cooking;

import com.cooking.api.OpenAIClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SimpleOpenAITest {
    public static void main(String[] args) {
        try {
            Config config = ConfigFactory.load();
            String apiKey = config.getString("cooking.openai.api-key");
            String model = config.getString("cooking.openai.model");
            double temperature = config.getDouble("cooking.openai.temperature");

            System.out.println("Testing OpenAI with key: " + apiKey.substring(0, 10) + "...");

            OpenAIClient client = new OpenAIClient(apiKey, model, temperature);
            String response = client.generateRecipe("Give me a simple recipe for scrambled eggs");

            System.out.println("✅ SUCCESS: " + response);

        } catch (Exception e) {
            System.err.println("❌ FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}