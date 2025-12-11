package com.example.springboot_api.controllers.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.services.shared.ai.AIModelService;

import lombok.RequiredArgsConstructor;

/**
 * Test Controller để thử nghiệm AI Model Service.
 * CHỈ DÙNG CHO DEV/TEST - KHÔNG DÙNG TRONG PRODUCTION!
 */
@RestController
@RequestMapping("/test/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AIModelService aiModelService;

    /**
     * Test Gemini model với REST API (giống Python)
     * GET /test/ai/gemini?prompt=Hello
     */
    @GetMapping("/gemini")
    public ResponseEntity<TestResponse> testGeminiGet(
            @RequestParam(defaultValue = "Xin chào, bạn là ai?") String prompt) {
        try {
            long startTime = System.currentTimeMillis();
            String response = aiModelService.callGeminiModel(prompt);
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(new TestResponse(
                    "success",
                    prompt,
                    response,
                    duration + "ms",
                    null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new TestResponse(
                    "error",
                    prompt,
                    null,
                    null,
                    e.getMessage()));
        }
    }

    /**
     * Test Gemini model với SDK (có thể bị rate limit khác)
     * GET /test/ai/gemini-sdk?prompt=Hello
     */
    @GetMapping("/gemini-sdk")
    public ResponseEntity<TestResponse> testGeminiSdkGet(
            @RequestParam(defaultValue = "Xin chào, bạn là ai?") String prompt) {
        try {
            long startTime = System.currentTimeMillis();
            String response = aiModelService.callGeminiModel(prompt);
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(new TestResponse(
                    "success",
                    prompt,
                    response,
                    duration + "ms",
                    null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new TestResponse(
                    "error",
                    prompt,
                    null,
                    null,
                    e.getMessage()));
        }
    }

    /**
     * Test Gemini model với prompt dài (POST)
     * POST /test/ai/gemini
     * Body: { "prompt": "..." }
     */
    @PostMapping("/gemini")
    public ResponseEntity<TestResponse> testGeminiPost(@RequestBody PromptRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            String response = aiModelService.callGeminiModel(request.prompt());
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(new TestResponse(
                    "success",
                    request.prompt(),
                    response,
                    duration + "ms",
                    null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new TestResponse(
                    "error",
                    request.prompt(),
                    null,
                    null,
                    e.getMessage()));
        }
    }

    /**
     * Test Groq model
     * GET /test/ai/groq?prompt=Hello
     */
    @GetMapping("/groq")
    public ResponseEntity<TestResponse> testGroqGet(
            @RequestParam(defaultValue = "Xin chào, bạn là ai?") String prompt) {
        try {
            long startTime = System.currentTimeMillis();
            String response = aiModelService.callGroqModel(prompt);
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(new TestResponse(
                    "success",
                    prompt,
                    response,
                    duration + "ms",
                    null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new TestResponse(
                    "error",
                    prompt,
                    null,
                    null,
                    e.getMessage()));
        }
    }

    /**
     * Health check - kiểm tra AI services có sẵn sàng không
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        boolean geminiOk = false;
        boolean groqOk = false;
        String geminiError = null;
        String groqError = null;

        // Test Gemini
        try {
            aiModelService.callGeminiModel("ping");
            geminiOk = true;
        } catch (Exception e) {
            geminiError = e.getMessage();
        }

        // Test Groq
        try {
            aiModelService.callGroqModel("ping");
            groqOk = true;
        } catch (Exception e) {
            groqError = e.getMessage();
        }

        return ResponseEntity.ok(new HealthResponse(geminiOk, geminiError, groqOk, groqError));
    }

    // DTOs
    public record PromptRequest(String prompt) {
    }

    public record TestResponse(
            String status,
            String prompt,
            String response,
            String duration,
            String error) {
    }

    public record HealthResponse(
            boolean geminiOk,
            String geminiError,
            boolean groqOk,
            String groqError) {
    }
}
