package com.cooking.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ConfigLoader {
    private static final Config config = ConfigFactory.load();

    public static String getOpenAIApiKey() {
        try {
            return config.getString("cooking.openai.api-key");
        } catch (Exception e) {
            return "";
        }
    }

    public static String getOpenAIModel() {
        try {
            return config.getString("cooking.openai.model");
        } catch (Exception e) {
            return "gpt-3.5-turbo";
        }
    }

    public static double getOpenAITemperature() {
        try {
            return config.getDouble("cooking.openai.temperature");
        } catch (Exception e) {
            return 0.7;
        }
    }

    public static int getMaxTokens() {
        try {
            return config.getInt("cooking.openai.max-tokens");
        } catch (Exception e) {
            return 1000;
        }
    }

    public static String getSystemName() {
        try {
            return config.getString("cooking.node.system-name");
        } catch (Exception e) {
            return "SmartCookingSystem";
        }
    }

    public static Config getConfig() {
        return config;
    }

    public static void printConfig() {
        System.out.println("Configuration loaded:");
        System.out.println("- OpenAI Model: " + getOpenAIModel());
        System.out.println("- Temperature: " + getOpenAITemperature());
        System.out.println("- Max Tokens: " + getMaxTokens());
        System.out.println("- System Name: " + getSystemName());
        System.out.println("- API Key configured: " + (!getOpenAIApiKey().isEmpty()));
    }
}