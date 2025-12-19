package com.example.springboot_api.controllers.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.services.shared.ai.generation.VideoGenerationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/slides")
@RequiredArgsConstructor
public class SlideTestController {

        private final VideoGenerationService videoGenerationService;

        // ================================
        // VIDEO GENERATION TEST
        // ================================

        /**
         * Test video generation - gọi TRỰC TIẾP VideoGenerationService.
         * POST /user/slides/video-test
         */
        @PostMapping("/video-test")
        public ResponseEntity<Map<String, Object>> testVideoGeneration(@RequestBody VideoTestRequest request) {
                try {
                        UUID aiSetId = UUID.randomUUID();
                        System.out.println("🎬 [TEST] Video generation started: " + aiSetId);

                        videoGenerationService.processVideoGenerationAsync(
                                        aiSetId,
                                        request.getNotebookId(),
                                        request.getUserId(),
                                        request.getFileIds(),
                                        request.getTemplateName() != null ? request.getTemplateName() : "CORPORATE",
                                        request.getAdditionalRequirements(),
                                        request.getNumberOfSlides() > 0 ? request.getNumberOfSlides() : 5,
                                        request.isGenerateImages());

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "aiSetId", aiSetId.toString(),
                                        "status", "processing",
                                        "message", "Video generation started"));
                } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.internalServerError().body(Map.of(
                                        "success", false,
                                        "error", e.getMessage()));
                }
        }

        /**
         * Quick video test.
         * GET /user/slides/video-quick?notebookId=...&userId=...&fileId=...
         */
        @GetMapping("/video-quick")
        public ResponseEntity<Map<String, Object>> quickVideoTest(
                        @RequestParam UUID notebookId,
                        @RequestParam UUID userId,
                        @RequestParam UUID fileId,
                        @RequestParam(required = false, defaultValue = "5") int slides,
                        @RequestParam(required = false, defaultValue = "false") boolean generateImages) {

                UUID aiSetId = UUID.randomUUID();
                System.out.println("🎬 [QUICK TEST] aiSetId: " + aiSetId);

                videoGenerationService.processVideoGenerationAsync(
                                aiSetId, notebookId, userId, List.of(fileId),
                                "CORPORATE", null, slides, generateImages);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "aiSetId", aiSetId.toString(),
                                "message", "Video generation started"));
        }

        @lombok.Data
        public static class VideoTestRequest {
                private UUID notebookId;
                private UUID userId;
                private List<UUID> fileIds;
                private String templateName;
                private int numberOfSlides;
                private String additionalRequirements;
                private boolean generateImages;
        }
}
