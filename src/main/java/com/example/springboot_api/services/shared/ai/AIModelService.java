package com.example.springboot_api.services.shared.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springboot_api.dto.shared.ai.GroqChatRequest;
import com.example.springboot_api.dto.shared.ai.GroqChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

/**
 * Service for calling AI models (Groq and Gemini).
 */
@Service
public class AIModelService {

    private static final Logger log = LoggerFactory.getLogger(AIModelService.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    private final com.google.genai.Client geminiClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String groqApiKey;

    public AIModelService(
            com.google.genai.Client geminiClient,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Qualifier("groqApiKey") String groqApiKey) {
        this.geminiClient = geminiClient;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.groqApiKey = groqApiKey;
    }

    /**
     * Calls Groq model with the provided prompt.
     */
    public String callGroqModel(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        try {
            GroqChatRequest request = GroqChatRequest.create(prompt.trim());
            String responseJson = webClient.post()
                    .uri(GROQ_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            GroqChatResponse response = objectMapper.readValue(responseJson, GroqChatResponse.class);
            String result = response.getText();

            return (result == null || result.isEmpty()) ? "No response generated" : result;

        } catch (Exception ex) {
            log.error("Error calling Groq API: {}", ex.getMessage());
            throw new RuntimeException("Error calling Groq API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Calls Gemini model with the provided prompt.
     */
    public String callGeminiModel(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }

        try {
            Content content = Content.fromParts(Part.fromText(prompt.trim()));
            GenerateContentResponse response = geminiClient.models.generateContent(
                    DEFAULT_GEMINI_MODEL,
                    java.util.List.of(content),
                    null);

            String result = response.text();
            return (result == null || result.isEmpty()) ? "No response generated" : result;

        } catch (Exception ex) {
            log.error("Error calling Gemini API: {}", ex.getMessage());
            throw new RuntimeException("Error calling Gemini API: " + ex.getMessage(), ex);
        }
    }
}
