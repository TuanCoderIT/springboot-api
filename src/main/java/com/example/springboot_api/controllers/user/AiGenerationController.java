package com.example.springboot_api.controllers.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.chatbot.AiSetResponse;
import com.example.springboot_api.services.user.AiGenerationService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho các tính năng AI Generation (Quiz, Summary, Flashcards, TTS,
 * Video...).
 * Base URL: /user/notebooks/{notebookId}/ai
 * 
 * Sử dụng NotebookAiSet để quản lý các AI generation sets.
 */
@RestController
@RequestMapping("/user/notebooks/{notebookId}/ai")
@RequiredArgsConstructor
public class AiGenerationController {

    private final AiGenerationService aiGenerationService;

    // ================================
    // QUIZ GENERATION
    // ================================

    /**
     * Tạo quiz từ các notebook files (chạy nền).
     * POST /user/notebooks/{notebookId}/ai/quiz/generate
     * 
     * @param user                   Current authenticated user
     * @param notebookId             Notebook ID
     * @param fileIds                Danh sách file IDs
     * @param numberOfQuestions      Số lượng câu hỏi: "few" | "standard" | "many"
     * @param difficultyLevel        Độ khó: "easy" | "medium" | "hard"
     * @param additionalRequirements Yêu cầu bổ sung (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    @PostMapping("/quiz/generate")
    public ResponseEntity<Map<String, Object>> generateQuiz(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false, defaultValue = "standard") String numberOfQuestions,
            @RequestParam(required = false, defaultValue = "medium") String difficultyLevel,
            @RequestParam(required = false) String additionalRequirements) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = aiGenerationService.generateQuiz(
                notebookId, user.getId(), fileIds, numberOfQuestions, difficultyLevel, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    // ================================
    // AI SETS (thay thế AI Tasks)
    // ================================

    /**
     * Lấy danh sách AI Sets theo notebook.
     * - Sets của user hiện tại: Hiển thị tất cả status
     * - Sets của người khác: Chỉ hiển thị done
     * 
     * GET /user/notebooks/{notebookId}/ai/sets?setType=quiz
     * 
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param setType    Loại set (optional): quiz, summary, flashcards, tts, video
     * @return List<AiSetResponse>
     */
    @GetMapping("/sets")
    public ResponseEntity<List<AiSetResponse>> getAiSets(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam(required = false) String setType) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        List<AiSetResponse> sets = aiGenerationService.getAiSets(notebookId, user.getId(), setType);

        return ResponseEntity.ok(sets);
    }

    // ================================
    // FUTURE: SUMMARY GENERATION
    // ================================

    // TODO: POST /user/notebooks/{notebookId}/ai/summary/generate

    // ================================
    // FUTURE: FLASHCARDS GENERATION
    // ================================

    // TODO: POST /user/notebooks/{notebookId}/ai/flashcards/generate

    // ================================
    // FUTURE: TTS GENERATION
    // ================================

    // TODO: POST /user/notebooks/{notebookId}/ai/tts/generate

    // ================================
    // FUTURE: VIDEO GENERATION
    // ================================

    // TODO: POST /user/notebooks/{notebookId}/ai/video/generate
}
