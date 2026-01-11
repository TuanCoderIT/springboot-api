package com.example.springboot_api.config.AI;

import org.springframework.beans.factory.annotation.Qualifier;
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
     * Bean cung cấp Gemini API key cho các service khác.
     * Inject bằng: @Qualifier("geminiApiKey") String apiKey
     */
    @Bean
    @Qualifier("geminiApiKey")
    public String geminiApiKey() {
        return resolveApiKey();
    }

    /**
     * Ưu tiên: ENV variable > application.yml
     */
    private String resolveApiKey() {

        // 3. Check application.yml
        if (this.apiKey != null && !this.apiKey.isEmpty()) {
            return this.apiKey;
        }

        return null;
    }
}