package com.example.springboot_api.dto.shared.ai;

/**
 * Response DTO for AI Model test endpoints.
 */
public record AIModelTestResponse(
        String status,
        String model,
        String prompt,
        String response,
        long timeMs,
        String error) {

    public static AIModelTestResponse success(String model, String prompt, String response, long timeMs) {
        return new AIModelTestResponse("success", model, prompt, response, timeMs, null);
    }

    public static AIModelTestResponse error(String model, String prompt, String error) {
        return new AIModelTestResponse("error", model, prompt, null, 0, error);
    }
}

