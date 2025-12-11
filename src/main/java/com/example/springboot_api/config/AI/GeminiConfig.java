package com.example.springboot_api.config.AI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${google.api.gemini_key:}")
    private String apiKey;

    @Bean
    public com.google.genai.Client geminiClient() {
        String keyToUse = resolveApiKey();

        if (keyToUse == null || keyToUse.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini API key not found. Please set GOOGLE_API_KEY or GEMINI_API_KEY environment variable, " +
                            "or configure google.api.gemini_key in application.yml with a valid API key.");
        }

        try {
            return com.google.genai.Client.builder()
                    .apiKey(keyToUse)
                    .build();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to create Gemini client: " + e.getMessage(), e);
        }
    }

    /**
     * Ưu tiên: ENV variable > application.yml
     */
    private String resolveApiKey() {
        // 1. Check GOOGLE_API_KEY
        String envKey = System.getenv("GOOGLE_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }

        // 2. Check GEMINI_API_KEY
        envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }

        // 3. Check application.yml
        if (this.apiKey != null && !this.apiKey.isEmpty()) {
            return this.apiKey;
        }

        return null;
    }
}