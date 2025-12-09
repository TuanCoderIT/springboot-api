package com.example.springboot_api.config.AI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${google.api.gemini_key:UNCONFIGURED_GEMINI_KEY}")
    private String apiKey;

    private static final String PLACEHOLDER_KEY = "AIzaSyCn7GDboHpG5vy0LE2HaiNwTSwFhvDW5MY";

    @Bean
    public com.google.genai.Client geminiClient() {
        String keyToUse = null;

        String envKey = System.getenv("GOOGLE_API_KEY");
        if (envKey == null || envKey.isEmpty()) {
            envKey = System.getenv("GEMINI_API_KEY");
        }

        if (envKey != null && !envKey.isEmpty()) {
            keyToUse = envKey;
        } else if (this.apiKey != null && !this.apiKey.isEmpty()
                && !"UNCONFIGURED_GEMINI_KEY".equals(this.apiKey)) {
            keyToUse = this.apiKey;
        }

        if (keyToUse == null || keyToUse.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini API key not found. Please set GOOGLE_API_KEY or GEMINI_API_KEY environment variable, " +
                            "or configure google.api.gemini_key in application.yml with a valid API key.");
        }

        if (PLACEHOLDER_KEY.equals(keyToUse)) {
            System.err.println(
                    "WARNING: Using placeholder Gemini API key. API calls will fail. Please configure a valid API key.");
        }

        try {
            return com.google.genai.Client.builder()
                    .apiKey(keyToUse)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to create Gemini client: " + e.getMessage(), e);
        }
    }
}