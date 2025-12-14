package com.example.springboot_api.controllers.user;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.services.shared.ai.AiImageGeneratorService;

import lombok.RequiredArgsConstructor;

/**
 * Controller test AI Image Generation.
 */
@RestController
@RequestMapping("/user/ai-images")
@RequiredArgsConstructor
public class AiImageController {

    private final AiImageGeneratorService imageGeneratorService;

    /**
     * Kiểm tra service đã được cấu hình chưa.
     * GET /user/ai-images/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "configured", imageGeneratorService.isConfigured(),
                "message", imageGeneratorService.isConfigured()
                        ? "AI Image Generator ready"
                        : "Missing gemini.api-key in config"));
    }

    /**
     * Sinh hình ảnh từ prompt.
     * POST /user/ai-images/generate
     * 
     * Body:
     * {
     * "prompt": "A cute cat playing with yarn"
     * }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateImage(@RequestBody GenerateRequest request) {
        if (!imageGeneratorService.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "AI Image service not configured. Set gemini.api-key in application.yml"));
        }

        try {
            String imageUrl = imageGeneratorService.generateAndSaveImage(request.getPrompt());

            if (imageUrl == null) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "success", false,
                        "error", "Failed to generate image"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "imageUrl", imageUrl,
                    "prompt", request.getPrompt()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Sinh hình ảnh và trả về base64.
     * POST /user/ai-images/generate-base64
     */
    @PostMapping("/generate-base64")
    public ResponseEntity<Map<String, Object>> generateImageBase64(@RequestBody GenerateRequest request) {
        if (!imageGeneratorService.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "AI Image service not configured"));
        }

        String base64 = imageGeneratorService.generateImageBase64(request.getPrompt());

        if (base64 == null) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to generate image"));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "base64Image", base64,
                "dataUrl", "data:image/png;base64," + base64));
    }

    /**
     * Sinh icon/illustration nhỏ.
     * POST /user/ai-images/generate-icon
     */
    @PostMapping("/generate-icon")
    public ResponseEntity<Map<String, Object>> generateIcon(@RequestBody GenerateRequest request) {
        if (!imageGeneratorService.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "AI Image service not configured"));
        }

        String base64 = imageGeneratorService.generateIconBase64(request.getPrompt());

        if (base64 == null) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to generate icon"));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "base64Icon", base64,
                "dataUrl", "data:image/png;base64," + base64));
    }

    @lombok.Data
    public static class GenerateRequest {
        private String prompt;
    }
}
