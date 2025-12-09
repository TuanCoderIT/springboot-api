package com.example.springboot_api.config.AI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Groq API.
 * Supports configuration via application.yml or GROQ_API_KEY environment variable.
 */
@Configuration
public class GroqConfig {

    @Value("${groq.api-key:UNCONFIGURED_GROQ_KEY}")
    private String apiKey;

    /**
     * Provides Groq API key with fallback priority:
     * 1. GROQ_API_KEY environment variable
     * 2. groq.api-key from application.yml
     * 
     * @return validated Groq API key
     * @throws IllegalStateException if no valid API key is found
     */
    @Bean
    public String groqApiKey() {
        String keyToUse = null;

        // Priority 1: Environment variable
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            keyToUse = envKey;
        } else if (this.apiKey != null && !this.apiKey.isEmpty()
                && !"UNCONFIGURED_GROQ_KEY".equals(this.apiKey)) {
            // Priority 2: application.yml
            keyToUse = this.apiKey;
        }

        if (keyToUse == null || keyToUse.isEmpty()) {
            throw new IllegalStateException(
                    "Groq API key not found. Please set GROQ_API_KEY environment variable, " +
                            "or configure groq.api-key in application.yml with a valid API key.");
        }

        return keyToUse;
    }
}

