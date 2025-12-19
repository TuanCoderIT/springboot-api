package com.example.springboot_api.controllers.user;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.ai.MindmapResponse;
import com.example.springboot_api.dto.user.audio.AudioListResponse;
import com.example.springboot_api.dto.user.chatbot.AiSetResponse;
import com.example.springboot_api.dto.user.flashcard.FlashcardListResponse;
import com.example.springboot_api.dto.user.quiz.QuizListResponse;
import com.example.springboot_api.dto.user.suggestion.SuggestionResponse;
import com.example.springboot_api.dto.user.summary.SummaryResponse;
import com.example.springboot_api.services.user.AiSetService;
import com.example.springboot_api.services.user.AudioService;
import com.example.springboot_api.services.user.FlashcardService;
import com.example.springboot_api.services.user.MindmapService;
import com.example.springboot_api.services.user.QuizService;
import com.example.springboot_api.services.user.SuggestionService;
import com.example.springboot_api.services.user.SummaryService;
import com.example.springboot_api.services.user.VideoService;

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

    private final AiSetService aiSetService;
    private final QuizService quizService;
    private final FlashcardService flashcardService;
    private final AudioService audioService;
    private final MindmapService mindmapService;
    private final SuggestionService suggestionService;
    private final SummaryService summaryService;
    private final VideoService videoService;

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

        Map<String, Object> result = quizService.generateQuiz(
                notebookId, user.getId(), fileIds, numberOfQuestions, difficultyLevel, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    // ================================
    // AUDIO OVERVIEW (SYNC TTS)
    // ================================

    /**
     * Tạo Audio Overview (script + ElevenLabs TTS) từ các file đã chọn.
     * POST /user/notebooks/{notebookId}/ai/audio-overview/generate
     *
     * @param user         Current authenticated user
     * @param notebookId   Notebook ID
     * @param fileIds      Danh sách file IDs
     * @param voiceId      Voice ElevenLabs (optional)
     * @param outputFormat Định dạng audio (vd: mp3_44100_128; optional)
     * @param notes        Yêu cầu bổ sung cho prompt (optional)
     * @return Map chứa audioUrl và thông tin thành công
     */
    @PostMapping("/audio-overview/generate")
    public ResponseEntity<Map<String, Object>> generateAudioOverview(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false) String voiceId,
            @RequestParam(required = false) String outputFormat,
            @RequestParam(required = false) String notes) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = audioService.generateAudioOverview(
                notebookId, user.getId(), fileIds, voiceId, outputFormat, notes);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    // ================================
    // AUDIO OVERVIEW (ASYNC, giống quiz)
    // ================================

    /**
     * Tạo Audio Overview chạy nền (giống quiz): trả aiSetId để poll.
     * POST /user/notebooks/{notebookId}/ai/audio-overview/generate-async
     */
    @PostMapping("/audio-overview/generate-async")
    public ResponseEntity<Map<String, Object>> generateAudioOverviewAsync(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false) String voiceId,
            @RequestParam(required = false) String outputFormat,
            @RequestParam(required = false) String notes) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = audioService.generateAudioOverview(
                notebookId, user.getId(), fileIds, voiceId, outputFormat, notes);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy chi tiết audio theo AI Set ID.
     * 
     * GET /user/notebooks/{notebookId}/ai/audio/{aiSetId}
     * 
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID chứa audio
     * @return AudioListResponse
     */
    @GetMapping("/audio/{aiSetId}")
    public ResponseEntity<AudioListResponse> getAudioDetails(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        AudioListResponse response = audioService.getAudioByAiSetId(user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy chi tiết quiz theo AI Set ID.
     * Bao gồm tất cả câu hỏi và câu trả lời.
     * 
     * GET /user/notebooks/{notebookId}/ai/quiz/{aiSetId}
     * 
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID chứa quiz
     * @return QuizListResponse
     */
    @GetMapping("/quiz/{aiSetId}")
    public ResponseEntity<QuizListResponse> getQuizDetails(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        QuizListResponse response = quizService.getQuizzesByAiSetId(user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
    }

    // ================================
    // FLASHCARD GENERATION
    // ================================

    /**
     * Tạo flashcards từ các notebook files (chạy nền).
     * POST /user/notebooks/{notebookId}/ai/flashcards/generate
     */
    @PostMapping("/flashcards/generate")
    public ResponseEntity<Map<String, Object>> generateFlashcards(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false, defaultValue = "standard") String numberOfCards,
            @RequestParam(required = false) String additionalRequirements) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = flashcardService.generateFlashcards(
                notebookId, user.getId(), fileIds, numberOfCards, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy danh sách flashcards theo AI Set ID.
     *
     * GET /user/notebooks/{notebookId}/ai/flashcards/{aiSetId}
     */
    @GetMapping("/flashcards/{aiSetId}")
    public ResponseEntity<FlashcardListResponse> getFlashcards(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        FlashcardListResponse response = flashcardService.getFlashcardsByAiSetId(user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
    }

    // ================================
    // MINDMAP GENERATION
    // ================================

    /**
     * Tạo mindmap từ các notebook files (chạy nền).
     * POST /user/notebooks/{notebookId}/ai/mindmap/generate
     *
     * @param user                   Current authenticated user
     * @param notebookId             Notebook ID
     * @param fileIds                Danh sách file IDs
     * @param additionalRequirements Yêu cầu bổ sung (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    @PostMapping("/mindmap/generate")
    public ResponseEntity<Map<String, Object>> generateMindmap(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false) String additionalRequirements) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = mindmapService.generateMindmap(
                notebookId, user.getId(), fileIds, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy mindmap theo AI Set ID.
     *
     * GET /user/notebooks/{notebookId}/ai/mindmap/{aiSetId}
     *
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID chứa mindmap
     * @return MindmapResponse
     */
    @GetMapping("/mindmap/{aiSetId}")
    public ResponseEntity<MindmapResponse> getMindmap(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        MindmapResponse response = mindmapService.getMindmapByAiSetId(user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
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

        List<AiSetResponse> sets = aiSetService.getAiSets(notebookId, user.getId(), setType);

        return ResponseEntity.ok(sets);
    }

    /**
     * Xóa AI Set.
     * Chỉ cho phép xóa nếu user là người tạo AI Set.
     * 
     * DELETE /user/notebooks/{notebookId}/ai/sets/{aiSetId}
     * 
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID cần xóa
     * @return 204 No Content nếu thành công
     */
    @DeleteMapping("/sets/{aiSetId}")
    public ResponseEntity<Void> deleteAiSet(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        aiSetService.deleteAiSet(user.getId(), aiSetId);

        return ResponseEntity.noContent().build();
    }

    // ================================
    // SUGGESTION GENERATION
    // ================================

    /**
     * Tạo suggestions từ các notebook files (chạy nền).
     * POST /user/notebooks/{notebookId}/ai/suggestions/generate
     */
    @PostMapping("/suggestions/generate")
    public ResponseEntity<Map<String, Object>> generateSuggestions(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false) String additionalRequirements) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = suggestionService.generateSuggestions(
                notebookId, user.getId(), fileIds, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy danh sách suggestions theo AI Set ID.
     * GET /user/notebooks/{notebookId}/ai/suggestions/{aiSetId}
     */
    @GetMapping("/suggestions/{aiSetId}")
    public ResponseEntity<SuggestionResponse> getSuggestions(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        SuggestionResponse response = suggestionService.getSuggestionsByAiSetId(user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
    }

    // ================================
    // VIDEO GENERATION
    // ================================

    /**
     * Tạo video từ các notebook files (chạy nền).
     * POST /user/notebooks/{notebookId}/ai/video/generate
     *
     * @param user                   Current authenticated user
     * @param notebookId             Notebook ID
     * @param fileIds                Danh sách file IDs
     * @param videoLength            Độ dài video: short (5), medium (10), long (15)
     *                               - default: medium
     * @param additionalRequirements Yêu cầu bổ sung (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    @PostMapping("/video/generate")
    public ResponseEntity<Map<String, Object>> generateVideo(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false, defaultValue = "medium") String videoLength,
            @RequestParam(required = false) String additionalRequirements) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        // Chuyển đổi videoLength sang số slides
        int numberOfSlides = switch (videoLength.toLowerCase()) {
            case "short" -> 5;
            case "long" -> 15;
            default -> 10; // medium
        };

        // generateImages luôn bật
        Map<String, Object> result = videoService.generateVideo(
                notebookId, user.getId(), fileIds, numberOfSlides, true, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy video theo AI Set ID.
     * GET /user/notebooks/{notebookId}/ai/video/{aiSetId}
     *
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID chứa video
     * @return VideoResponse
     */
    @GetMapping("/video/{aiSetId}")
    public ResponseEntity<com.example.springboot_api.dto.user.video.VideoResponse> getVideo(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        com.example.springboot_api.dto.user.video.VideoResponse response = videoService.getVideoByAiSetId(
                user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
    }

    // ================================
    // SUMMARY GENERATION
    // ================================

    /**
     * Tạo summary từ các notebook files (chạy nền).
     * POST /user/notebooks/{notebookId}/ai/summary/generate
     *
     * @param user                   Current authenticated user
     * @param notebookId             Notebook ID
     * @param fileIds                Danh sách file IDs
     * @param voiceId                Giọng đọc TTS (optional, null = không tạo
     *                               audio)
     * @param language               Ngôn ngữ (vi, en...), default: vi
     * @param additionalRequirements Yêu cầu bổ sung (optional)
     * @return Map chứa aiSetId để track tiến trình
     */
    @PostMapping("/summary/generate")
    public ResponseEntity<Map<String, Object>> generateSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @RequestParam List<UUID> fileIds,
            @RequestParam(required = false) String voiceId,
            @RequestParam(required = false, defaultValue = "vi") String language,
            @RequestParam(required = false) String additionalRequirements) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        if (fileIds == null || fileIds.isEmpty()) {
            throw new BadRequestException("Danh sách file IDs không được để trống");
        }

        Map<String, Object> result = summaryService.generateSummary(
                notebookId, user.getId(), fileIds, voiceId, language, additionalRequirements);

        if (result.containsKey("error")) {
            throw new BadRequestException((String) result.get("error"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy summary theo AI Set ID.
     * GET /user/notebooks/{notebookId}/ai/summary/{aiSetId}
     *
     * @param user       Current authenticated user
     * @param notebookId Notebook ID
     * @param aiSetId    AI Set ID chứa summary
     * @return SummaryResponse
     */
    @GetMapping("/summary/{aiSetId}")
    public ResponseEntity<SummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        SummaryResponse response = summaryService.getSummaryByAiSetId(user.getId(), notebookId, aiSetId);

        return ResponseEntity.ok(response);
    }

}
