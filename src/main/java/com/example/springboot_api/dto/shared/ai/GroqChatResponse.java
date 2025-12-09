package com.example.springboot_api.dto.shared.ai;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Groq Chat API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroqChatResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices,
        Usage usage) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            Integer index,
            Message message,
            @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String role,
            String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }

    /**
     * Extracts the text content from the first choice.
     * 
     * @return the generated text content, or empty string if not available
     */
    public String getText() {
        return Optional.ofNullable(choices)
                .filter(c -> !c.isEmpty())
                .map(c -> c.get(0))
                .map(Choice::message)
                .map(Message::content)
                .orElse("");
    }
}

